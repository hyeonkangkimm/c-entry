package com.example.gonggong.domain.analysis.openai;

import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.example.gonggong.domain.analysis.service.ProductNormalizer;
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
public class OpenAiProductNormalizeClient implements ProductNormalizer {

	private static final Logger log = LoggerFactory.getLogger(OpenAiProductNormalizeClient.class);

	private final OpenAiProperties properties;
	private final ProductNormalizePromptBuilder promptBuilder;
	private final OpenAiTransport transport;
	private final ObjectMapper objectMapper;

	@Autowired
	public OpenAiProductNormalizeClient(
		@Value("${openai.api-key:}") String apiKey,
		@Value("${openai.model:gpt-5.5}") String model,
		@Value("${openai.responses-api-url:https://api.openai.com/v1/responses}") String responsesApiUrl,
		ProductNormalizePromptBuilder promptBuilder,
		OpenAiTransport transport,
		ObjectMapper objectMapper
	) {
		this(new OpenAiProperties(apiKey, model, responsesApiUrl), promptBuilder, transport, objectMapper);
	}

	OpenAiProductNormalizeClient(
		OpenAiProperties properties,
		ProductNormalizePromptBuilder promptBuilder,
		OpenAiTransport transport,
		ObjectMapper objectMapper
	) {
		this.properties = properties;
		this.promptBuilder = promptBuilder;
		this.transport = transport;
		this.objectMapper = objectMapper;
	}

	public ProductNormalizeResult normalize(ProductAnalyzeRequest request) {
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			throw new AnalysisException(AnalysisErrorCode.OPENAI_API_KEY_MISSING);
		}

		log.info("OpenAI product normalization started model={}", properties.model());
		String responseBody = transport.postJson(
			properties.responsesApiUrl(),
			properties.apiKey(),
			buildRequestBody(promptBuilder.build(request))
		);
		ProductNormalizeResult result = parseResponse(responseBody);
		log.info(
			"OpenAI product normalization completed standardProductName={} primaryProductName={} productForm={} brandName={} category={} matchedRecallProductName={} modelName={} searchKeywords={} primarySearchKeywords={} kcCertificationSearchKeywords={} hskCandidateKeywords={} riskIngredientKeywords={} confidence={}",
			result.standardProductName(),
			result.primaryProductName(),
			result.productForm(),
			result.brandName(),
			result.category(),
			result.matchedRecallProductName(),
			result.modelName(),
			result.searchKeywords(),
			result.primarySearchKeywords(),
			result.kcCertificationSearchKeywords(),
			result.hskCandidateKeywords(),
			result.riskIngredientKeywords(),
			result.confidence()
		);
		return result;
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
						"name", "product_normalization_result",
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
			"required", List.of(
				"standardProductName",
				"searchKeywords",
				"brandName",
				"category",
				"matchedRecallProductName",
				"modelName",
				"barcodeNum",
				"certNum",
				"materialKeywords",
				"targetUser",
				"riskIngredientKeywords",
				"hskCandidateKeywords",
				"primaryProductName",
				"productForm",
				"primarySearchKeywords",
				"kcCertificationSearchKeywords",
				"componentKeywords",
				"featureKeywords",
				"confidence"
			),
			"properties", Map.ofEntries(
				Map.entry("standardProductName", Map.of("type", List.of("string", "null"))),
				Map.entry("searchKeywords", arrayOfStringsSchema()),
				Map.entry("brandName", Map.of("type", List.of("string", "null"))),
				Map.entry("category", Map.of("type", List.of("string", "null"))),
				Map.entry("matchedRecallProductName", Map.of("type", List.of("string", "null"))),
				Map.entry("modelName", Map.of("type", List.of("string", "null"))),
				Map.entry("barcodeNum", Map.of("type", List.of("string", "null"))),
				Map.entry("certNum", Map.of("type", List.of("string", "null"))),
				Map.entry("materialKeywords", arrayOfStringsSchema()),
				Map.entry("targetUser", Map.of("type", List.of("string", "null"))),
				Map.entry("riskIngredientKeywords", arrayOfStringsSchema()),
				Map.entry("hskCandidateKeywords", arrayOfStringsSchema()),
				Map.entry("primaryProductName", Map.of("type", List.of("string", "null"))),
				Map.entry("productForm", Map.of(
					"type", "string",
					"enum", List.of("FINISHED_PRODUCT", "PART", "ACCESSORY", "MATERIAL", "SET", "UNKNOWN")
				)),
				Map.entry("primarySearchKeywords", arrayOfStringsSchema()),
				Map.entry("kcCertificationSearchKeywords", arrayOfStringsSchema()),
				Map.entry("componentKeywords", arrayOfStringsSchema()),
				Map.entry("featureKeywords", arrayOfStringsSchema()),
				Map.entry("confidence", Map.of("type", "number", "minimum", 0, "maximum", 1))
			)
		);
	}

	private Map<String, Object> arrayOfStringsSchema() {
		return Map.of(
			"type", "array",
			"items", Map.of("type", "string")
		);
	}

	private ProductNormalizeResult parseResponse(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			String outputText = extractOutputText(root);
			log.info("OpenAI product normalization raw output preview={}", preview(outputText));
			return objectMapper.readValue(outputText, ProductNormalizeResult.class);
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

	private String preview(String value) {
		if (value == null || value.isBlank()) {
			return "(empty)";
		}
		String normalized = value.replaceAll("\\s+", " ").trim();
		return normalized.length() <= 400 ? normalized : normalized.substring(0, 400);
	}
}
