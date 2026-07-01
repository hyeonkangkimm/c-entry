package com.example.gonggong.domain.analysis.openai;

import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallItem;
import com.example.gonggong.domain.analysis.service.RecallRelevanceDecider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiRecallRelevanceDecider implements RecallRelevanceDecider {

	private static final Logger log = LoggerFactory.getLogger(OpenAiRecallRelevanceDecider.class);
	private static final int MAX_CANDIDATES = 8;
	private static final int MAX_CANDIDATE_LOG_ITEMS = 5;

	private final OpenAiProperties properties;
	private final OpenAiTransport transport;
	private final ObjectMapper objectMapper;

	@Autowired
	public OpenAiRecallRelevanceDecider(
		@Value("${openai.api-key:}") String apiKey,
		@Value("${openai.model:gpt-5.5}") String model,
		@Value("${openai.responses-api-url:https://api.openai.com/v1/responses}") String responsesApiUrl,
		OpenAiTransport transport,
		ObjectMapper objectMapper
	) {
		this(new OpenAiProperties(apiKey, model, responsesApiUrl), transport, objectMapper);
	}

	OpenAiRecallRelevanceDecider(
		OpenAiProperties properties,
		OpenAiTransport transport,
		ObjectMapper objectMapper
	) {
		this.properties = properties;
		this.transport = transport;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<SafetyKoreaRecallItem> selectRelevant(ProductNormalizeResult normalized, List<SafetyKoreaRecallItem> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			log.info("OpenAI recall relevance skipped reason=no-candidates");
			return List.of();
		}
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			log.info("OpenAI recall relevance skipped reason=missing-openai-api-key candidateCount={}", candidates.size());
			return candidates;
		}

		List<SafetyKoreaRecallItem> limitedCandidates = candidates.stream()
			.limit(MAX_CANDIDATES)
			.toList();
		Map<String, SafetyKoreaRecallItem> candidateById = candidateById(limitedCandidates);
		log.info(
			"OpenAI recall relevance started standardProductName={} primaryProductName={} productForm={} searchKeywords={} candidateCount={}",
			normalized.standardProductName(),
			normalized.primaryProductName(),
			normalized.productForm(),
			safeList(normalized.searchKeywords()),
			candidateById.size()
		);
		int loggedCount = 0;
		for (Map.Entry<String, SafetyKoreaRecallItem> entry : candidateById.entrySet()) {
			if (loggedCount >= MAX_CANDIDATE_LOG_ITEMS) {
				break;
			}
			SafetyKoreaRecallItem item = entry.getValue();
			log.info(
				"OpenAI recall relevance candidate id={} source={} recallUid={} recallProductName={} recallBrandName={} recallModelName={} matchedQuery={}",
				entry.getKey(),
				item.source(),
				item.recallUid(),
				item.recallProductName(),
				item.recallBrandName(),
				item.recallModelName(),
				item.matchedQuery()
			);
			loggedCount += 1;
		}
		if (candidateById.size() > loggedCount) {
			log.info("OpenAI recall relevance candidate log truncated loggedCount={} skippedCount={}", loggedCount, candidateById.size() - loggedCount);
		}
		String responseBody = transport.postJson(
			properties.responsesApiUrl(),
			properties.apiKey(),
			buildRequestBody(buildPrompt(normalized, candidateById))
		);
		List<String> relevantIds = parseRelevantCandidateIds(responseBody);
		log.info(
			"OpenAI recall relevance completed relevantCandidateIds={} relevantCount={} candidateCount={}",
			relevantIds,
			relevantIds.size(),
			candidateById.size()
		);
		List<SafetyKoreaRecallItem> relevant = new ArrayList<>();
		for (String relevantId : relevantIds) {
			SafetyKoreaRecallItem item = candidateById.get(relevantId);
			if (item != null) {
				relevant.add(item);
			}
		}
		return relevant;
	}

	private Map<String, SafetyKoreaRecallItem> candidateById(List<SafetyKoreaRecallItem> candidates) {
		Map<String, SafetyKoreaRecallItem> candidateById = new LinkedHashMap<>();
		for (int index = 0; index < candidates.size(); index += 1) {
			candidateById.put("c" + index, candidates.get(index));
		}
		return candidateById;
	}

	private String buildPrompt(ProductNormalizeResult normalized, Map<String, SafetyKoreaRecallItem> candidateById) {
		StringBuilder prompt = new StringBuilder("""
			You decide whether SafetyKorea recall candidates refer to the same real product type as the analyzed product.
			Return only candidate ids that are actually relevant.

			Rules:
			- Match the real product subject, not words used only as shape/design/appearance.
			- "keyboard keycap shaped keyring" is not relevant to a keyboard product.
			- "watch shaped hand warmer" is not relevant to a wrist watch product.
			- If the recall product is an accessory, toy, keyring, charm, case, or ornament and the analyzed product is the main device, reject it.
			- If all candidates are unrelated, return an empty relevantCandidateIds array.

			ANALYZED_PRODUCT:
			""");
		prompt.append("- standardProductName: ").append(nullToEmpty(normalized.standardProductName())).append('\n');
		prompt.append("- primaryProductName: ").append(nullToEmpty(normalized.primaryProductName())).append('\n');
		prompt.append("- productForm: ").append(nullToEmpty(normalized.productForm())).append('\n');
		prompt.append("- primarySearchKeywords: ").append(safeList(normalized.primarySearchKeywords())).append('\n');
		prompt.append("- searchKeywords: ").append(safeList(normalized.searchKeywords())).append('\n');
		prompt.append("- brandName: ").append(nullToEmpty(normalized.brandName())).append('\n');
		prompt.append("- modelName: ").append(nullToEmpty(normalized.modelName())).append('\n');
		prompt.append("\nRECALL_CANDIDATES:\n");
		for (Map.Entry<String, SafetyKoreaRecallItem> entry : candidateById.entrySet()) {
			SafetyKoreaRecallItem item = entry.getValue();
			prompt.append("- id: ").append(entry.getKey()).append('\n');
			prompt.append("  source: ").append(item.source()).append('\n');
			prompt.append("  recallProductName: ").append(nullToEmpty(item.recallProductName())).append('\n');
			prompt.append("  recallBrandName: ").append(nullToEmpty(item.recallBrandName())).append('\n');
			prompt.append("  recallModelName: ").append(nullToEmpty(item.recallModelName())).append('\n');
			prompt.append("  reason: ").append(nullToEmpty(firstNonBlank(item.harmDscr(), item.accidentCaseDscr()))).append('\n');
		}
		return prompt.toString();
	}

	private String buildRequestBody(String prompt) {
		try {
			return objectMapper.writeValueAsString(Map.of(
				"model", properties.model(),
				"input", List.of(Map.of(
					"role", "user",
					"content", prompt
				)),
				"text", Map.of(
					"format", Map.of(
						"type", "json_schema",
						"name", "recall_relevance_result",
						"strict", true,
						"schema", responseSchema()
					)
				)
			));
		} catch (JsonProcessingException exception) {
			throw new AnalysisException(AnalysisErrorCode.OPENAI_RESPONSE_PARSE_FAILED, exception);
		}
	}

	private Map<String, Object> responseSchema() {
		return Map.of(
			"type", "object",
			"additionalProperties", false,
			"required", List.of("relevantCandidateIds"),
			"properties", Map.of(
				"relevantCandidateIds", Map.of(
					"type", "array",
					"items", Map.of("type", "string")
				)
			)
		);
	}

	private List<String> parseRelevantCandidateIds(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			JsonNode relevantIds = objectMapper.readTree(extractOutputText(root)).get("relevantCandidateIds");
			if (relevantIds == null || !relevantIds.isArray()) {
				return List.of();
			}
			List<String> ids = new ArrayList<>();
			for (JsonNode relevantId : relevantIds) {
				if (relevantId.isTextual()) {
					ids.add(relevantId.asText());
				}
			}
			return ids;
		} catch (JsonProcessingException exception) {
			throw new AnalysisException(AnalysisErrorCode.OPENAI_RESPONSE_PARSE_FAILED, exception);
		}
	}

	private String extractOutputText(JsonNode root) {
		JsonNode outputText = root.get("output_text");
		if (outputText != null && outputText.isTextual()) {
			return outputText.asText();
		}
		JsonNode output = root.get("output");
		if (output != null && output.isArray()) {
			for (JsonNode outputItem : output) {
				JsonNode content = outputItem.get("content");
				if (content == null || !content.isArray()) {
					continue;
				}
				for (JsonNode contentItem : content) {
					JsonNode text = contentItem.get("text");
					if (text != null && text.isTextual()) {
						return text.asText();
					}
				}
			}
		}
		throw new AnalysisException(AnalysisErrorCode.OPENAI_RESPONSE_PARSE_FAILED);
	}

	private List<String> safeList(List<String> values) {
		return values == null ? List.of() : values;
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		return fallback;
	}
}
