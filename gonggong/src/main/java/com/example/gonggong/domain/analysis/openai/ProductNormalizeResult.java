package com.example.gonggong.domain.analysis.openai;

import com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate;

import java.util.List;

public record ProductNormalizeResult(
	String standardProductName,
	List<String> searchKeywords,
	String brandName,
	String category,
	String matchedRecallProductName,
	String modelName,
	String barcodeNum,
	String certNum,
	List<String> materialKeywords,
	String targetUser,
	List<String> riskIngredientKeywords,
	List<String> hskCandidateKeywords,
	String primaryProductName,
	String productForm,
	List<String> primarySearchKeywords,
	List<String> kcCertificationSearchKeywords,
	List<String> componentKeywords,
	List<String> featureKeywords,
	List<ChemicalIngredientCandidate> chemicalCandidates,
	double confidence
) {
	public ProductNormalizeResult {
		searchKeywords = copyStrings(searchKeywords);
		materialKeywords = copyStrings(materialKeywords);
		riskIngredientKeywords = copyStrings(riskIngredientKeywords);
		hskCandidateKeywords = copyStrings(hskCandidateKeywords);
		primarySearchKeywords = copyStrings(primarySearchKeywords);
		kcCertificationSearchKeywords = copyStrings(kcCertificationSearchKeywords);
		componentKeywords = copyStrings(componentKeywords);
		featureKeywords = copyStrings(featureKeywords);
		chemicalCandidates = normalizeChemicalCandidates(chemicalCandidates, riskIngredientKeywords);
	}

	public ProductNormalizeResult(
		String standardProductName,
		List<String> searchKeywords,
		String brandName,
		String category,
		String matchedRecallProductName,
		String modelName,
		String barcodeNum,
		String certNum,
		List<String> materialKeywords,
		String targetUser,
		List<String> riskIngredientKeywords,
		List<String> hskCandidateKeywords,
		String primaryProductName,
		String productForm,
		List<String> primarySearchKeywords,
		List<String> kcCertificationSearchKeywords,
		List<String> componentKeywords,
		List<String> featureKeywords,
		double confidence
	) {
		this(
			standardProductName,
			searchKeywords,
			brandName,
			category,
			matchedRecallProductName,
			modelName,
			barcodeNum,
			certNum,
			materialKeywords,
			targetUser,
			riskIngredientKeywords,
			hskCandidateKeywords,
			primaryProductName,
			productForm,
			primarySearchKeywords,
			kcCertificationSearchKeywords,
			componentKeywords,
			featureKeywords,
			null,
			confidence
		);
	}

	public ProductNormalizeResult(
		String standardProductName,
		List<String> searchKeywords,
		String brandName,
		String category,
		String matchedRecallProductName,
		List<String> materialKeywords,
		String targetUser,
		List<String> hskCandidateKeywords,
		List<String> riskIngredientKeywords,
		double confidence
	) {
		this(
			standardProductName,
			searchKeywords,
			brandName,
			category,
			matchedRecallProductName,
			null,
			null,
			null,
			materialKeywords,
			targetUser,
			riskIngredientKeywords,
			hskCandidateKeywords,
			standardProductName,
			"UNKNOWN",
			hskCandidateKeywords,
			List.of(),
			List.of(),
			List.of(),
			null,
			confidence
		);
	}

	public ProductNormalizeResult(
		String standardProductName,
		List<String> searchKeywords,
		String brandName,
		String category,
		String matchedRecallProductName,
		String modelName,
		String barcodeNum,
		String certNum,
		List<String> materialKeywords,
		String targetUser,
		List<String> riskIngredientKeywords,
		List<String> hskCandidateKeywords,
		double confidence
	) {
		this(
			standardProductName,
			searchKeywords,
			brandName,
			category,
			matchedRecallProductName,
			modelName,
			barcodeNum,
			certNum,
			materialKeywords,
			targetUser,
			riskIngredientKeywords,
			hskCandidateKeywords,
			standardProductName,
			"UNKNOWN",
			hskCandidateKeywords,
			List.of(),
			List.of(),
			List.of(),
			null,
			confidence
		);
	}

	public ProductNormalizeResult(
		String standardProductName,
		List<String> searchKeywords,
		String brandName,
		String category,
		String matchedRecallProductName,
		List<String> materialKeywords,
		String targetUser,
		List<String> riskIngredientKeywords,
		List<String> hskCandidateKeywords,
		List<String> primarySearchKeywords,
		List<String> kcCertificationSearchKeywords,
		double confidence
	) {
		this(
			standardProductName,
			searchKeywords,
			brandName,
			category,
			matchedRecallProductName,
			null,
			null,
			null,
			materialKeywords,
			targetUser,
			riskIngredientKeywords,
			hskCandidateKeywords,
			standardProductName,
			"UNKNOWN",
			primarySearchKeywords,
			kcCertificationSearchKeywords,
			List.of(),
			List.of(),
			null,
			confidence
		);
	}

	private static List<String> copyStrings(List<String> values) {
		return values == null ? List.of() : values.stream()
			.filter(value -> value != null && !value.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
	}

	private static List<ChemicalIngredientCandidate> normalizeChemicalCandidates(
		List<ChemicalIngredientCandidate> candidates,
		List<String> fallbackNames
	) {
		List<ChemicalIngredientCandidate> cleaned = candidates == null ? List.of() : candidates.stream()
			.filter(candidate -> candidate != null && candidate.name() != null && !candidate.name().isBlank())
			.toList();
		if (!cleaned.isEmpty()) {
			return List.copyOf(cleaned);
		}
		return fallbackNames == null ? List.of() : fallbackNames.stream()
			.filter(name -> name != null && !name.isBlank())
			.map(name -> new ChemicalIngredientCandidate(name, null, null))
			.toList();
	}
}
