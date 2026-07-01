package com.example.gonggong.domain.risk.chemical;

public record ChemicalIngredientCandidate(
	String name,
	String casNumber,
	String englishName
) {
	public ChemicalIngredientCandidate {
		name = normalize(name);
		casNumber = normalize(casNumber);
		englishName = normalize(englishName);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
