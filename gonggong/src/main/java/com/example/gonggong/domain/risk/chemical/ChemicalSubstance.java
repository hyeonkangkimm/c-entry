package com.example.gonggong.domain.risk.chemical;

import java.util.List;

public record ChemicalSubstance(
	String koreanName,
	String englishName,
	String casNumber,
	List<ChemicalClassification> classifications
) {
	public ChemicalSubstance {
		classifications = classifications == null ? List.of() : List.copyOf(classifications);
	}
}
