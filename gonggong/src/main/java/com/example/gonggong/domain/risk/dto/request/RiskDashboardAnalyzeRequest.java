package com.example.gonggong.domain.risk.dto.request;

import com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

public record RiskDashboardAnalyzeRequest(
	@Pattern(regexp = "\\d{10}", message = "HSK Code는 숫자 10자리여야 합니다.")
	String hskCode,
	@NotBlank(message = "상품명은 필수입니다.")
	String productName,
	String productDescription,
	List<String> ingredients,
	List<ChemicalIngredientCandidate> chemicalCandidates,
	String originCountry,
	@DecimalMin(value = "0", message = "신고가격은 0 이상이어야 합니다.")
	BigDecimal declaredValue,
	String currency,
	@Min(value = 1, message = "수량은 1 이상이어야 합니다.")
	Integer quantity,
	@DecimalMin(value = "0", message = "운임은 0 이상이어야 합니다.")
	BigDecimal shippingCost,
	@DecimalMin(value = "0", message = "보험료는 0 이상이어야 합니다.")
	BigDecimal insuranceCost,
	String kcCertificationNumber,
	String kcCertificationType,
	String modelName,
	String brandName,
	String standardProductName,
	String primaryProductName,
	List<String> primarySearchKeywords,
	List<String> kcCertificationSearchKeywords,
	List<PrefilteredRecallRequest> prefilteredRecalls
) {
	public RiskDashboardAnalyzeRequest(
		String hskCode,
		String productName,
		String productDescription,
		List<String> ingredients,
		String originCountry,
		BigDecimal declaredValue,
		String currency,
		Integer quantity,
		BigDecimal shippingCost,
		BigDecimal insuranceCost,
		String kcCertificationNumber
	) {
		this(
			hskCode,
			productName,
			productDescription,
			ingredients,
			List.of(),
			originCountry,
			declaredValue,
			currency,
			quantity,
			shippingCost,
			insuranceCost,
			kcCertificationNumber,
			null,
			null,
			null,
			null,
			null,
			List.of(),
			List.of(),
			List.of()
		);
	}

	public RiskDashboardAnalyzeRequest(
		String hskCode,
		String productName,
		String productDescription,
		List<String> ingredients,
		List<ChemicalIngredientCandidate> chemicalCandidates,
		String originCountry,
		BigDecimal declaredValue,
		String currency,
		Integer quantity,
		BigDecimal shippingCost,
		BigDecimal insuranceCost,
		String kcCertificationNumber
	) {
		this(
			hskCode,
			productName,
			productDescription,
			ingredients,
			chemicalCandidates,
			originCountry,
			declaredValue,
			currency,
			quantity,
			shippingCost,
			insuranceCost,
			kcCertificationNumber,
			null,
			null,
			null,
			null,
			null,
			List.of(),
			List.of(),
			List.of()
		);
	}

	public RiskDashboardAnalyzeRequest(
		String hskCode,
		String productName,
		String productDescription,
		List<String> ingredients,
		String originCountry,
		BigDecimal declaredValue,
		String currency,
		Integer quantity,
		BigDecimal shippingCost,
		BigDecimal insuranceCost,
		String kcCertificationNumber,
		String standardProductName,
		String primaryProductName,
		List<String> primarySearchKeywords
	) {
		this(
			hskCode,
			productName,
			productDescription,
			ingredients,
			List.of(),
			originCountry,
			declaredValue,
			currency,
			quantity,
			shippingCost,
			insuranceCost,
			kcCertificationNumber,
			null,
			null,
			null,
			standardProductName,
			primaryProductName,
			primarySearchKeywords,
			List.of(),
			List.of()
		);
	}

	public RiskDashboardAnalyzeRequest(
		String hskCode,
		String productName,
		String productDescription,
		List<String> ingredients,
		String originCountry,
		BigDecimal declaredValue,
		String currency,
		Integer quantity,
		BigDecimal shippingCost,
		BigDecimal insuranceCost,
		String kcCertificationNumber,
		String standardProductName,
		String primaryProductName,
		List<String> primarySearchKeywords,
		List<PrefilteredRecallRequest> prefilteredRecalls
	) {
		this(
			hskCode,
			productName,
			productDescription,
			ingredients,
			List.of(),
			originCountry,
			declaredValue,
			currency,
			quantity,
			shippingCost,
			insuranceCost,
			kcCertificationNumber,
			null,
			null,
			null,
			standardProductName,
			primaryProductName,
			primarySearchKeywords,
			List.of(),
			prefilteredRecalls
		);
	}

	public List<String> normalizedIngredients() {
		if (chemicalCandidates != null) {
			List<String> normalizedChemicalNames = normalizedChemicalCandidates().stream()
				.map(ChemicalIngredientCandidate::name)
				.toList();
			if (!normalizedChemicalNames.isEmpty()) {
				return normalizedChemicalNames;
			}
		}
		return copyStrings(ingredients);
	}

	public List<ChemicalIngredientCandidate> normalizedChemicalCandidates() {
		if (chemicalCandidates != null) {
			List<ChemicalIngredientCandidate> normalizedCandidates = chemicalCandidates.stream()
				.filter(candidate -> candidate != null && candidate.name() != null && !candidate.name().isBlank())
				.toList();
			if (!normalizedCandidates.isEmpty()) {
				return normalizedCandidates;
			}
		}
		return copyStrings(ingredients).stream()
			.map(name -> new ChemicalIngredientCandidate(name, null, null))
			.toList();
	}

	public RiskDashboardAnalyzeRequest withChemicalCandidates(List<ChemicalIngredientCandidate> candidates) {
		return new RiskDashboardAnalyzeRequest(
			hskCode,
			productName,
			productDescription,
			ingredients,
			candidates,
			originCountry,
			declaredValue,
			currency,
			quantity,
			shippingCost,
			insuranceCost,
			kcCertificationNumber,
			kcCertificationType,
			modelName,
			brandName,
			standardProductName,
			primaryProductName,
			primarySearchKeywords,
			kcCertificationSearchKeywords,
			prefilteredRecalls
		);
	}

	public List<String> normalizedRecallKeywords() {
		List<String> normalizedKeywords = primarySearchKeywords == null ? List.of() : primarySearchKeywords.stream()
			.filter(keyword -> keyword != null && !keyword.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
		if (!normalizedKeywords.isEmpty()) {
			return normalizedKeywords;
		}
		return Stream.of(primaryProductName, standardProductName, productName, productDescription)
			.filter(keyword -> keyword != null && !keyword.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
	}

	public List<String> normalizedKcCertificationSearchKeywords() {
		List<String> normalizedKeywords = kcCertificationSearchKeywords == null ? List.of() : kcCertificationSearchKeywords.stream()
			.filter(keyword -> keyword != null && !keyword.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
		if (!normalizedKeywords.isEmpty()) {
			return normalizedKeywords;
		}
		List<String> primaryKeywords = primarySearchKeywords == null ? List.of() : primarySearchKeywords.stream()
			.filter(keyword -> keyword != null && !keyword.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
		if (!primaryKeywords.isEmpty()) {
			return primaryKeywords;
		}
		return Stream.of(primaryProductName, standardProductName, productName)
			.filter(keyword -> keyword != null && !keyword.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
	}

	public String normalizedKcCertificationType() {
		if (kcCertificationType != null && !kcCertificationType.isBlank()) {
			return kcCertificationType.trim();
		}
		String text = String.join(" ",
			productDescription == null ? "" : productDescription,
			productName == null ? "" : productName
		).replaceAll("\\s+", " ").trim();
		if (text.isBlank() || !text.contains("KC")) {
			return null;
		}
		java.util.regex.Matcher directMatcher = java.util.regex.Pattern
			.compile("KC\\s*인증\\s*([가-힣A-Za-z0-9ㆍ·\\s]{2,40}?(?:안전확인|안전인증|공급자적합성|적합성평가|전파인증))")
			.matcher(text);
		if (directMatcher.find()) {
			return cleanupKcCertificationType(directMatcher.group(1));
		}
		java.util.regex.Matcher typeMatcher = java.util.regex.Pattern
			.compile("((?:전기용품|생활용품|전기용품\\s*및\\s*생활용품|어린이제품)[가-힣A-Za-z0-9ㆍ·\\s]{0,30}?(?:안전확인|안전인증|공급자적합성|적합성평가|전파인증))")
			.matcher(text);
		if (typeMatcher.find()) {
			return cleanupKcCertificationType(typeMatcher.group(1));
		}
		if (text.contains("KC인증") || text.contains("KC 인증")) {
			return "KC 인증";
		}
		return null;
	}

	private String cleanupKcCertificationType(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String cleaned = value.replaceAll("[|/,:;()\\[\\]{}]+$", "")
			.replaceAll("\\s+", " ")
			.trim();
		return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
	}

	public List<PrefilteredRecallRequest> normalizedPrefilteredRecalls() {
		return prefilteredRecalls == null ? List.of() : prefilteredRecalls.stream()
			.filter(recall -> recall != null && recall.recallProductName() != null && !recall.recallProductName().isBlank())
			.toList();
	}

	public BigDecimal safeDeclaredValue() {
		return declaredValue == null ? BigDecimal.ZERO : declaredValue;
	}

	public BigDecimal safeShippingCost() {
		return shippingCost == null ? BigDecimal.ZERO : shippingCost;
	}

	public BigDecimal safeInsuranceCost() {
		return insuranceCost == null ? BigDecimal.ZERO : insuranceCost;
	}

	private static List<String> copyStrings(List<String> values) {
		return values == null ? List.of() : values.stream()
			.filter(value -> value != null && !value.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
	}
}
