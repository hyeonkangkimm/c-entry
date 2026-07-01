package com.example.gonggong.domain.risk.chemical;

import java.time.LocalDate;

public record ChemicalRegulationRule(
	String classificationIdentifier,
	String classificationName,
	boolean regulated,
	String lawName,
	String obligationArticle,
	String obligationSourceUrl,
	String penaltyArticle,
	String penaltyText,
	String penaltySourceUrl,
	LocalDate effectiveFrom,
	LocalDate effectiveTo
) {
	public boolean activeOn(LocalDate date) {
		return (effectiveFrom == null || !date.isBefore(effectiveFrom))
			&& (effectiveTo == null || !date.isAfter(effectiveTo));
	}

	public String relatedLaw() {
		return join(lawName, obligationArticle);
	}

	public String verifiedPenalty() {
		if (!hasText(penaltyArticle) || !hasText(penaltyText) || !officialLawUrl(penaltySourceUrl)) {
			return null;
		}
		return join(penaltyArticle, penaltyText);
	}

	private String join(String first, String second) {
		if (!hasText(first)) return hasText(second) ? second.trim() : null;
		return hasText(second) ? first.trim() + " " + second.trim() : first.trim();
	}

	private boolean officialLawUrl(String value) {
		return hasText(value) && value.startsWith("https://www.law.go.kr/");
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
