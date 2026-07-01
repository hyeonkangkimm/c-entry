package com.example.gonggong.domain.risk.chemical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ChemicalApiJsonParser {

	private static final Logger log = LoggerFactory.getLogger(ChemicalApiJsonParser.class);

	private final ObjectMapper objectMapper;

	public ChemicalApiJsonParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ChemicalLookupResult parse(String body, String ingredient) {
		try {
			JsonNode root = objectMapper.readTree(body);
			JsonNode response = root.has("response") ? root.path("response") : root;
			log.info("Chemical API parse started ingredient={} hasResponseNode={} bodyLength={}",
				ingredient,
				root.has("response"),
				body == null ? 0 : body.length());
			if (!response.has("header") || !response.has("body")) {
				log.info("Chemical API parse unavailable ingredient={} reason=unrecognized-response-schema", ingredient);
				return ChemicalLookupResult.unavailable(ingredient, "unrecognized-response-schema");
			}
			String resultCode = text(response.path("header"), "resultCode");
			if (hasText(resultCode) && !"00".equals(resultCode) && !"200".equals(resultCode)) {
				log.info("Chemical API parse unavailable ingredient={} reason=api-result-{}", ingredient, resultCode);
				return ChemicalLookupResult.unavailable(ingredient, "api-result-" + resultCode);
			}
			JsonNode items = response.path("body").path("items");
			if (items.isMissingNode() || items.isNull() || (items.isArray() && items.isEmpty())) {
				log.info("Chemical API parse notFound ingredient={} reason=empty-items", ingredient);
				return ChemicalLookupResult.notFound(ingredient);
			}
			JsonNode item = selectBestItem(items, ingredient);
			if (item == null || !item.isObject()) {
				log.info("Chemical API parse notFound ingredient={} reason=no-match", ingredient);
				return ChemicalLookupResult.notFound(ingredient);
			}
			log.info("Chemical API parse matched ingredient={} matchedCas={} matchedKorean={} matchedEnglish={}",
				ingredient,
				text(item, "casNo"),
				text(item, "sbstnNmKor"),
				text(item, "sbstnNmEng"));
			return ChemicalLookupResult.matched(ingredient, toSubstance(item));
		}
		catch (JsonProcessingException exception) {
			log.info("Chemical API parse unavailable ingredient={} reason=invalid-json", ingredient);
			return ChemicalLookupResult.unavailable(ingredient, "invalid-json");
		}
	}

	private JsonNode selectBestItem(JsonNode items, String ingredient) {
		JsonNode exactMatch = selectExactItem(items, ingredient);
		if (exactMatch != null) {
			return exactMatch;
		}
		return selectPartialItem(items, ingredient);
	}

	private JsonNode selectExactItem(JsonNode items, String ingredient) {
		if (items.isObject()) {
			return matches(items, ingredient) ? items : null;
		}
		if (!items.isArray()) return null;
		for (JsonNode item : items) {
			if (matches(item, ingredient)) return item;
		}
		return null;
	}

	private JsonNode selectPartialItem(JsonNode items, String ingredient) {
		if (items.isObject()) {
			return partialMatches(items, ingredient) ? items : null;
		}
		if (!items.isArray()) return null;
		for (JsonNode item : items) {
			if (partialMatches(item, ingredient)) return item;
		}
		return null;
	}

	private boolean matches(JsonNode item, String ingredient) {
		String expected = normalize(ingredient);
		return List.of("casNo", "sbstnNmKor", "sbstnNmEng", "sbstnNm2Kor", "sbstnNm2Eng")
			.stream()
			.map(name -> text(item, name))
			.anyMatch(value -> expected.equals(normalize(value)));
	}

	private boolean partialMatches(JsonNode item, String ingredient) {
		String expected = normalize(ingredient);
		return List.of("casNo", "sbstnNmKor", "sbstnNmEng", "sbstnNm2Kor", "sbstnNm2Eng")
			.stream()
			.map(name -> text(item, name))
			.filter(this::hasText)
			.map(this::normalize)
			.anyMatch(value -> value.equals(expected) || value.contains(expected) || expected.contains(value));
	}

	private String normalize(String value) {
		return value == null ? "" : value.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
	}

	private ChemicalSubstance toSubstance(JsonNode item) {
		return new ChemicalSubstance(
			firstText(item, "sbstnNmKor", "chemNmKr", "chemNmKor", "korChemNm", "chemNameKr"),
			firstText(item, "sbstnNmEng", "chemNmEn", "chemNmEng", "engChemNm", "chemNameEn"),
			firstText(item, "casNo", "casNumber", "casNum"),
			classifications(firstNode(item, "typeList", "chemSbstnClList", "classifications", "chemClassList"))
		);
	}

	private List<ChemicalClassification> classifications(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return List.of();
		}
		List<ChemicalClassification> result = new ArrayList<>();
		if (node.isArray()) {
			for (JsonNode item : node) {
				result.add(toClassification(item));
			}
		}
		else if (node.isObject()) {
			result.add(toClassification(node));
		}
		return List.copyOf(result);
	}

	private ChemicalClassification toClassification(JsonNode node) {
		return new ChemicalClassification(
			firstText(node, "sbstnClsfTypeNm", "chemSbstnClType", "classificationType", "chemClType"),
			firstText(node, "unqNo", "chemSbstnClNo", "classificationNo", "uniqueNo"),
			firstText(node, "contInfo", "concentration", "contentInfo"),
			firstText(node, "excpInfo", "exceptInfo", "exceptionInfo"),
			firstText(node, "ancmntYmd", "ntfcDt", "noticeDate"),
			firstText(node, "ancmntInfo", "ntfcInfo", "noticeInformation", "noticeInfo")
		);
	}

	private JsonNode firstNode(JsonNode node, String... names) {
		for (String name : names) {
			JsonNode value = node.get(name);
			if (value != null && !value.isNull()) {
				return value;
			}
		}
		return null;
	}

	private String firstText(JsonNode node, String... names) {
		for (String name : names) {
			String value = text(node, name);
			if (hasText(value)) {
				return value.trim();
			}
		}
		return null;
	}

	private String text(JsonNode node, String name) {
		JsonNode value = node == null ? null : node.get(name);
		return value == null || value.isNull() ? null : value.asText();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
