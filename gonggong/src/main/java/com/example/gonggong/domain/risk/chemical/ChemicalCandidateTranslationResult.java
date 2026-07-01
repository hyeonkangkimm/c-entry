package com.example.gonggong.domain.risk.chemical;

import java.util.List;

public record ChemicalCandidateTranslationResult(
	List<ChemicalIngredientCandidate> chemicalCandidates
) {
	public ChemicalCandidateTranslationResult {
		chemicalCandidates = chemicalCandidates == null ? List.of() : List.copyOf(chemicalCandidates);
	}
}
