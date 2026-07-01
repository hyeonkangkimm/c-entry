package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class HskVectorQueryBuilder {

	public static final int DOCUMENT_VERSION = 4;

	public String buildQuery(HskMatchRequest request, HskFeatures features) {
		Set<String> parts = new LinkedHashSet<>();
		add(parts, features.primaryProductName());
		addAll(parts, features.primarySearchKeywords());
		addAll(parts, request.hskCandidateKeywords());
		addAll(parts, features.searchKeywords());
		add(parts, features.standardProductName());
		return String.join("\n", parts);
	}

	public String buildDocumentText(com.example.gonggong.domain.hsk.domain.HskItem item) {
		return String.join("\n",
			"HSK code: " + item.getHskCode(),
			"Korean item name: " + item.getKoreanName(),
			"Display item path: " + item.getDisplayName(),
			"English item name: " + item.getEnglishName(),
			"Classification context: " + classificationContext(item)
		);
	}

	private String classificationContext(com.example.gonggong.domain.hsk.domain.HskItem item) {
		String hskCode = item.getHskCode() == null ? "" : item.getHskCode();
		if (hskCode.startsWith("9101") || hskCode.startsWith("91021") || hskCode.startsWith("91022")) {
			return "손목시계 휴대용 시계 시계 wrist watch wrist-watch pocket watch watch";
		}
		if (hskCode.startsWith("841451") || hskCode.startsWith("841459")) {
			return "선풍기 전기선풍기 탁상용 선풍기 휴대용 선풍기 클립형 선풍기 팬 송풍기 electric fan table fan desk fan portable fan clip fan";
		}
		return "";
	}

	private void addAll(Set<String> parts, List<String> values) {
		if (values == null) {
			return;
		}
		values.forEach(value -> add(parts, value));
	}

	private void add(Set<String> parts, String value) {
		if (value != null && !value.isBlank()) {
			String trimmed = value.trim();
			parts.add(trimmed);
			addEquivalentTerms(parts, trimmed);
		}
	}

	private void addEquivalentTerms(Set<String> parts, String value) {
		String normalized = value.toLowerCase(Locale.ROOT);
		if (normalized.contains("손목시계") || normalized.contains("벽시계") || normalized.contains("시계")
			|| normalized.contains("wrist watch") || normalized.contains("wrist-watch")
			|| normalized.equals("watch") || normalized.equals("clock")) {
			parts.add("시계");
			parts.add("watch");
			parts.add("clock");
		}
		if (normalized.contains("선풍기") || normalized.equals("팬") || normalized.contains("electric fan")
			|| normalized.contains("table fan") || normalized.contains("desk fan") || normalized.contains("portable fan")) {
			parts.add("팬");
			parts.add("선풍기");
			parts.add("전기선풍기");
			parts.add("탁상용 선풍기");
			parts.add("electric fan");
			parts.add("table fan");
			parts.add("desk fan");
		}
	}
}
