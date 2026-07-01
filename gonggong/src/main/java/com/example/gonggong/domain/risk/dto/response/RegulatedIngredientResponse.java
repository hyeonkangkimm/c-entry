package com.example.gonggong.domain.risk.dto.response;

public record RegulatedIngredientResponse(
	String ingredientName,
	String casNumber,
	String hazardClassification,
	String relatedLaw,
	String penaltyProvision,
	String legalSourceUrl,
	String penaltySourceUrl,
	boolean regulated
) {
	public RegulatedIngredientResponse(
		String ingredientName,
		String casNumber,
		String hazardClassification,
		String relatedLaw,
		String penaltyProvision
	) {
		this(ingredientName, casNumber, hazardClassification, relatedLaw, penaltyProvision, null, null, true);
	}

	public RegulatedIngredientResponse(
		String ingredientName,
		String casNumber,
		String hazardClassification,
		String relatedLaw,
		String penaltyProvision,
		String legalSourceUrl,
		String penaltySourceUrl
	) {
		this(ingredientName, casNumber, hazardClassification, relatedLaw, penaltyProvision, legalSourceUrl, penaltySourceUrl, true);
	}
}
