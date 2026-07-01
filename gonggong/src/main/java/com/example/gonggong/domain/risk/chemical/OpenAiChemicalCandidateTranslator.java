package com.example.gonggong.domain.risk.chemical;

import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.example.gonggong.domain.analysis.openai.OpenAiProperties;
import com.example.gonggong.domain.analysis.openai.OpenAiTransport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiChemicalCandidateTranslator {

	private static final Logger log = LoggerFactory.getLogger(OpenAiChemicalCandidateTranslator.class);

	private final OpenAiProperties properties;
	private final ChemicalCandidatePromptBuilder promptBuilder;
	private final OpenAiTransport transport;
	private final ObjectMapper objectMapper;

	@Autowired
	public OpenAiChemicalCandidateTranslator(
		@Value("${openai.api-key:}") String apiKey,
		@Value("${openai.model:gpt-5.5}") String model,
		@Value("${openai.responses-api-url:https://api.openai.com/v1/responses}") String responsesApiUrl,
		ChemicalCandidatePromptBuilder promptBuilder,
		OpenAiTransport transport,
		ObjectMapper objectMapper
	) {
		this(new OpenAiProperties(apiKey, model, responsesApiUrl), promptBuilder, transport, objectMapper);
	}

	OpenAiChemicalCandidateTranslator(
		OpenAiProperties properties,
		ChemicalCandidatePromptBuilder promptBuilder,
		OpenAiTransport transport,
		ObjectMapper objectMapper
	) {
		this.properties = properties;
		this.promptBuilder = promptBuilder;
		this.transport = transport;
		this.objectMapper = objectMapper;
	}

	public List<ChemicalIngredientCandidate> translate(List<String> ingredients) {
		if (ingredients == null || ingredients.isEmpty()) {
			return List.of();
		}
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			log.info("OpenAI chemical candidate translation skipped reason=missing-openai-api-key ingredientCount={}", ingredients.size());
			return List.of();
		}

		log.info("OpenAI chemical candidate translation started ingredientCount={}", ingredients.size());
		String responseBody = transport.postJson(
			properties.responsesApiUrl(),
			properties.apiKey(),
			buildRequestBody(promptBuilder.build(ingredients))
		);
		ChemicalCandidateTranslationResult result = parseResponse(responseBody);
		log.info("OpenAI chemical candidate translation completed candidateCount={} candidates={}",
			result.chemicalCandidates().size(),
			result.chemicalCandidates());
		return result.chemicalCandidates();
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
						"name", "chemical_candidate_translation_result",
						"strict", true,
						"schema", responseSchema()
					)
				)
			));
		}
		catch (JsonProcessingException exception) {
			throw new AnalysisException(AnalysisErrorCode.OPENAI_RESPONSE_PARSE_FAILED, exception);
		}
	}

	private Map<String, Object> responseSchema() {
		return Map.of(
			"type", "object",
			"additionalProperties", false,
			"required", List.of("chemicalCandidates"),
			"properties", Map.of(
				"chemicalCandidates", Map.of(
					"type", "array",
					"maxItems", 5,
					"items", Map.of(
						"type", "object",
						"additionalProperties", false,
						"required", List.of("name", "casNumber", "englishName"),
						"properties", Map.of(
							"name", Map.of("type", List.of("string", "null")),
							"casNumber", Map.of("type", List.of("string", "null")),
							"englishName", Map.of("type", List.of("string", "null"))
						)
					)
				)
			)
		);
	}

	private ChemicalCandidateTranslationResult parseResponse(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			String outputText = extractOutputText(root);
			return objectMapper.readValue(outputText, ChemicalCandidateTranslationResult.class);
		}
		catch (JsonProcessingException exception) {
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
}
