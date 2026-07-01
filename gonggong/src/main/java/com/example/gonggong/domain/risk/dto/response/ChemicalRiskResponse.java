package com.example.gonggong.domain.risk.dto.response;

import com.example.gonggong.domain.risk.domain.RiskStatus;

import java.util.List;

public record ChemicalRiskResponse(
	RiskStatus status,
	int score,
	String message,
	List<RegulatedIngredientResponse> regulatedIngredients,
	String searchUrl,
	List<String> unanalyzedIngredients,
	boolean analysisUnavailable,
	String searchButtonText
) {
	public ChemicalRiskResponse(
		RiskStatus status,
		int score,
		String message,
		List<RegulatedIngredientResponse> regulatedIngredients,
		String searchUrl
	) {
		this(status, score, message, regulatedIngredients, searchUrl, List.of(), false,
			"화학 물질 종합정보시스템에서 직접 성분 검색하기");
	}

	public ChemicalRiskResponse {
		regulatedIngredients = regulatedIngredients == null ? List.of() : List.copyOf(regulatedIngredients);
		unanalyzedIngredients = unanalyzedIngredients == null ? List.of() : List.copyOf(unanalyzedIngredients);
	}
}
