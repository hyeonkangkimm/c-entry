package com.example.gonggong.domain.risk.chemical;

public record ChemicalLookupResult(
	ChemicalLookupStatus status,
	String requestedIngredient,
	ChemicalSubstance substance,
	String reason
) {
	public static ChemicalLookupResult matched(String ingredient, ChemicalSubstance substance) {
		return new ChemicalLookupResult(ChemicalLookupStatus.MATCHED, ingredient, substance, null);
	}

	public static ChemicalLookupResult notFound(String ingredient) {
		return new ChemicalLookupResult(ChemicalLookupStatus.NOT_FOUND, ingredient, null, null);
	}

	public static ChemicalLookupResult unavailable(String ingredient, String reason) {
		return new ChemicalLookupResult(ChemicalLookupStatus.UNAVAILABLE, ingredient, null, reason);
	}
}
