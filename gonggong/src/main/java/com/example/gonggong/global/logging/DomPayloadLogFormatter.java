package com.example.gonggong.global.logging;

import java.util.List;

public final class DomPayloadLogFormatter {

	private DomPayloadLogFormatter() {
	}

	public static String maskCertificationNumber(String certificationNumber) {
		if (certificationNumber == null || certificationNumber.isBlank()) {
			return null;
		}
		String trimmed = certificationNumber.trim();
		if (trimmed.length() <= 4) {
			return "****";
		}
		return trimmed.substring(0, Math.min(2, trimmed.length()))
			+ "****"
			+ trimmed.substring(Math.max(0, trimmed.length() - 2));
	}

	public static String clip(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String normalized = value.replaceAll("\\s+", " ").trim();
		if (normalized.length() <= maxLength) {
			return normalized;
		}
		return normalized.substring(0, Math.max(0, maxLength)) + "...";
	}

	public static List<String> clipList(List<String> values, int maxItems, int maxLength) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return values.stream()
			.filter(value -> value != null && !value.isBlank())
			.limit(maxItems)
			.map(value -> clip(value, maxLength))
			.toList();
	}
}
