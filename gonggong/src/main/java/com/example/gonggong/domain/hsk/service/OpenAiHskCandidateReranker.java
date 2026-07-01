package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.example.gonggong.domain.analysis.openai.OpenAiProperties;
import com.example.gonggong.domain.analysis.openai.OpenAiTransport;
import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Primary
public class OpenAiHskCandidateReranker implements HskCandidateReranker {

	private static final Logger log = LoggerFactory.getLogger(OpenAiHskCandidateReranker.class);
	private static final int MAX_CANDIDATES = 20;

	private final OpenAiProperties properties;
	private final OpenAiTransport transport;
	private final ObjectMapper objectMapper;

	@Autowired
	public OpenAiHskCandidateReranker(
		@Value("${openai.api-key:}") String apiKey,
		@Value("${openai.model:gpt-5.5}") String model,
		@Value("${openai.responses-api-url:https://api.openai.com/v1/responses}") String responsesApiUrl,
		OpenAiTransport transport,
		ObjectMapper objectMapper
	) {
		this(new OpenAiProperties(apiKey, model, responsesApiUrl), transport, objectMapper);
	}

	OpenAiHskCandidateReranker(
		OpenAiProperties properties,
		OpenAiTransport transport,
		ObjectMapper objectMapper
	) {
		this.properties = properties;
		this.transport = transport;
		this.objectMapper = objectMapper;
	}

	@Override
	public HskRerankResult rerank(HskMatchRequest request, HskFeatures features, List<HskVectorCandidate> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			return null;
		}

		List<HskVectorCandidate> limitedCandidates = candidates.stream()
			.limit(MAX_CANDIDATES)
			.toList();
		log.info(
			"OpenAI HSK rerank started primaryProductName={} productForm={} candidateCount={} topHskCodes={}",
			features.primaryProductName(),
			features.productForm(),
			limitedCandidates.size(),
			limitedCandidates.stream().map(candidate -> candidate.item().getHskCode()).limit(10).toList()
		);
		String responseBody = transport.postJson(
			properties.responsesApiUrl(),
			properties.apiKey(),
			buildRequestBody(buildPrompt(request, features, limitedCandidates))
		);
		HskRerankResult result = parseResult(responseBody);
		if (!candidateCodes(limitedCandidates).contains(result.selectedHskCode())) {
			log.info("OpenAI HSK rerank rejected selectedHskCode={} because it is not in vector candidates", result.selectedHskCode());
			return HskRerankResult.notSelected();
		}
		log.info(
			"OpenAI HSK rerank completed selectedHskCode={} confidence={} reason={}",
			result.selectedHskCode(),
			result.confidence(),
			result.reason()
		);
		return result;
	}

	private String buildPrompt(HskMatchRequest request, HskFeatures features, List<HskVectorCandidate> candidates) {
		StringBuilder prompt = new StringBuilder("""
			You select the best Korean customs HSK code from the provided candidates.
			You must not invent a code.
			You must choose only one hskCode from HSK_CANDIDATES, or return null if no candidate matches.

			Decision rules:
			- Do not choose the least-bad candidate just because candidates were provided.
			- If every candidate has a different function, use, product form, or customs meaning from the sold product, selectedHskCode must be null.
			- You may choose a broader or narrower candidate when it clearly covers the same real product function and use, such as a wrist watch matching a watch or clock category.
			- A common commercial name may map to an official customs category when the candidate displayName clearly covers that product.
			- For example, chicken feet may match edible poultry offal or other edible offal of chicken, but must not match chicken leg, chicken wing, live chicken, feed, or poultry machinery.
			- If preservation state is present, prefer the matching state. For example, frozen chicken feet should prefer frozen edible poultry offal over fresh or chilled offal.
			- Match the real product being sold, not materials, colors, features, included parts, advertising phrases, or shape/design words.
			- If the product is a finished product, reject manufacturing materials or parts unless the listing clearly sells only that part.
			- Prefer the candidate whose displayName, Korean item name, or English item name matches the actual use and function of the product.
			- For customs/tariff risk, accuracy is more important than returning a candidate.

			PRODUCT:
			""");
		prompt.append("- productName: ").append(nullToEmpty(request.productName())).append('\n');
		prompt.append("- description: ").append(nullToEmpty(request.description())).append('\n');
		prompt.append("- standardProductName: ").append(nullToEmpty(features.standardProductName())).append('\n');
		prompt.append("- primaryProductName: ").append(nullToEmpty(features.primaryProductName())).append('\n');
		prompt.append("- productForm: ").append(nullToEmpty(features.productForm())).append('\n');
		prompt.append("- primarySearchKeywords: ").append(safeList(features.primarySearchKeywords())).append('\n');
		prompt.append("- hskCandidateKeywords: ").append(safeList(request.hskCandidateKeywords())).append('\n');
		prompt.append("\nHSK_CANDIDATES:\n");
		for (HskVectorCandidate candidate : candidates) {
			HskItem item = candidate.item();
			prompt.append("- hskCode: ").append(item.getHskCode()).append('\n');
			prompt.append("  koreanName: ").append(nullToEmpty(item.getKoreanName())).append('\n');
			prompt.append("  displayName: ").append(nullToEmpty(item.getDisplayName())).append('\n');
			prompt.append("  englishName: ").append(nullToEmpty(item.getEnglishName())).append('\n');
			prompt.append("  vectorSimilarity: ").append(candidate.similarity()).append('\n');
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
						"name", "hsk_rerank_result",
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
		Map<String, Object> selectedHskCodeSchema = new LinkedHashMap<>();
		selectedHskCodeSchema.put("type", List.of("string", "null"));
		Map<String, Object> confidenceSchema = new LinkedHashMap<>();
		confidenceSchema.put("type", "number");
		confidenceSchema.put("minimum", 0);
		confidenceSchema.put("maximum", 1);
		Map<String, Object> reasonSchema = new LinkedHashMap<>();
		reasonSchema.put("type", List.of("string", "null"));

		return Map.of(
			"type", "object",
			"additionalProperties", false,
			"required", List.of("selectedHskCode", "confidence", "reason"),
			"properties", Map.of(
				"selectedHskCode", selectedHskCodeSchema,
				"confidence", confidenceSchema,
				"reason", reasonSchema
			)
		);
	}

	private HskRerankResult parseResult(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			JsonNode result = objectMapper.readTree(extractOutputText(root));
			JsonNode selectedHskCode = result.get("selectedHskCode");
			String hskCode = selectedHskCode == null || selectedHskCode.isNull() ? null : selectedHskCode.asText();
			double confidence = result.path("confidence").asDouble(0.0);
			JsonNode reason = result.get("reason");
			String reasonText = reason == null || reason.isNull() ? null : reason.asText();
			return new HskRerankResult(hskCode, confidence, reasonText);
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

	private Set<String> candidateCodes(List<HskVectorCandidate> candidates) {
		return candidates.stream()
			.map(candidate -> candidate.item().getHskCode())
			.collect(Collectors.toSet());
	}

	private List<String> safeList(List<String> values) {
		return values == null ? List.of() : values;
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
