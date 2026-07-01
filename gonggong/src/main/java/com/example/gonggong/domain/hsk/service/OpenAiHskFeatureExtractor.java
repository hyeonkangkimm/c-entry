package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;
import com.example.gonggong.domain.analysis.service.ProductNormalizer;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiHskFeatureExtractor implements HskFeatureExtractor {

	private final ProductNormalizer productNormalizer;

	public OpenAiHskFeatureExtractor(ProductNormalizer productNormalizer) {
		this.productNormalizer = productNormalizer;
	}

	@Override
	public HskFeatures extract(HskMatchRequest request) {
		if (hasProvidedNormalizedFeatures(request)) {
			return new HskFeatures(
				request.standardProductName(),
				mergeKeywords(
					request.standardProductName(),
					request.hskCandidateKeywords(),
					request.primarySearchKeywords()
				),
				firstNonBlank(request.primaryProductName(), request.standardProductName()),
				firstNonBlank(request.productForm(), "UNKNOWN"),
				safeList(request.primarySearchKeywords()),
				safeList(request.componentKeywords()),
				safeList(request.featureKeywords())
			);
		}

		ProductNormalizeResult normalized = productNormalizer.normalize(new ProductAnalyzeRequest(
			request.productName(),
			request.description(),
			request.imageUrl(),
			null,
			"hsk-match"
		));

		List<String> keywords = new ArrayList<>();
		keywords.add(normalized.standardProductName());
		if (normalized.hskCandidateKeywords() != null) {
			keywords.addAll(normalized.hskCandidateKeywords());
		}
		if (normalized.materialKeywords() != null) {
			keywords.addAll(normalized.materialKeywords());
		}
		if (normalized.searchKeywords() != null) {
			keywords.addAll(normalized.searchKeywords());
		}
		return new HskFeatures(
			normalized.standardProductName(),
			keywords.stream()
				.filter(keyword -> keyword != null && !keyword.isBlank())
				.map(String::trim)
				.distinct()
				.toList(),
			firstNonBlank(normalized.primaryProductName(), normalized.standardProductName()),
			firstNonBlank(normalized.productForm(), "UNKNOWN"),
			normalized.primarySearchKeywords() == null ? List.of() : normalized.primarySearchKeywords(),
			normalized.componentKeywords() == null ? List.of() : normalized.componentKeywords(),
			normalized.featureKeywords() == null ? List.of() : normalized.featureKeywords()
		);
	}

	private boolean hasProvidedNormalizedFeatures(HskMatchRequest request) {
		return request.standardProductName() != null && !request.standardProductName().isBlank()
			&& request.primaryProductName() != null && !request.primaryProductName().isBlank()
			&& request.primarySearchKeywords() != null && !request.primarySearchKeywords().isEmpty();
	}

	private List<String> mergeKeywords(
		String standardProductName,
		List<String> hskCandidateKeywords,
		List<String> primarySearchKeywords
	) {
		List<String> keywords = new ArrayList<>();
		keywords.add(standardProductName);
		keywords.addAll(safeList(primarySearchKeywords));
		keywords.addAll(safeList(hskCandidateKeywords));
		return keywords.stream()
			.filter(keyword -> keyword != null && !keyword.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
	}

	private List<String> safeList(List<String> values) {
		return values == null ? List.of() : values;
	}

	private String firstNonBlank(String primary, String fallback) {
		return primary == null || primary.isBlank() ? fallback : primary;
	}
}
