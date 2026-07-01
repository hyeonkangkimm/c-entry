package com.example.gonggong.domain.analysis.dto;

import com.example.gonggong.domain.analysis.RiskLevel;
import com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate;

import java.util.List;

public record ProductAnalyzeResponse(
	RiskLevel riskLevel,
	int riskScore,
	String category,
	String brandName,
	String modelName,
	String certNum,
	String standardProductName,
	List<String> hskCandidateKeywords,
	String primaryProductName,
	String productForm,
	List<String> primarySearchKeywords,
	List<String> kcCertificationSearchKeywords,
	List<String> componentKeywords,
	List<String> featureKeywords,
	String recallReason,
	List<String> harmfulIngredients,
	List<ChemicalIngredientCandidate> chemicalCandidates,
	List<MatchedRecallDto> matchedRecalls,
	String message
) {
	public ProductAnalyzeResponse {
		harmfulIngredients = harmfulIngredients == null ? List.of() : List.copyOf(harmfulIngredients);
		chemicalCandidates = chemicalCandidates == null ? List.of() : List.copyOf(chemicalCandidates);
		matchedRecalls = matchedRecalls == null ? List.of() : List.copyOf(matchedRecalls);
	}
}
