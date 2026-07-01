package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.dto.HskCandidateResponse;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.example.gonggong.domain.hsk.dto.HskMatchResponse;
import com.example.gonggong.domain.hsk.repository.HskItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HskMatchService {

	private static final int MAX_CANDIDATES = 5;
	private static final int VECTOR_SEARCH_LIMIT = 20;

	private final HskFeatureExtractor featureExtractor;
	private final HskVectorCandidateSearcher candidateSearcher;
	private final HskCandidateReranker candidateReranker;

	@Autowired
	public HskMatchService(
		HskFeatureExtractor featureExtractor,
		HskVectorCandidateSearcher candidateSearcher,
		HskCandidateReranker candidateReranker
	) {
		this.featureExtractor = featureExtractor;
		this.candidateSearcher = candidateSearcher;
		this.candidateReranker = candidateReranker;
	}

	public HskMatchService(HskFeatureExtractor featureExtractor, HskVectorCandidateSearcher candidateSearcher) {
		this(featureExtractor, candidateSearcher, HskCandidateReranker.keepVectorOrder());
	}

	HskMatchService(HskFeatureExtractor featureExtractor, HskItemReader itemReader) {
		this(featureExtractor, (request, features, limit) -> candidateSearchTerms(request, features).stream()
			.flatMap(term -> itemReader.findCandidates(term).stream())
			.distinct()
			.limit(limit)
			.map(item -> new HskVectorCandidate(item, 0.50))
			.toList(), HskCandidateReranker.keepVectorOrder());
	}

	public HskMatchResponse match(HskMatchRequest request) {
		HskFeatures features = featureExtractor.extract(request);
		List<HskVectorCandidate> vectorCandidates = candidateSearcher.search(request, features, VECTOR_SEARCH_LIMIT);
		HskRerankResult rerankResult = candidateReranker.rerank(request, features, vectorCandidates);
		List<HskCandidateResponse> candidates = vectorCandidates.stream()
			.map(candidate -> toCandidate(candidate, request, features))
			.map(candidate -> applyRerankResult(candidate, rerankResult))
			.sorted(Comparator.comparingDouble(HskCandidateResponse::confidence).reversed()
				.thenComparing(HskCandidateResponse::hskCode))
			.limit(MAX_CANDIDATES)
			.toList();

		if (candidates.isEmpty()) {
			return new HskMatchResponse(
				false,
				List.of(),
				"공식 관세청 HSK 품목 데이터에서 일치 후보를 찾지 못했습니다."
			);
		}
		if (rerankResult != null && !rerankResult.selected()) {
			return new HskMatchResponse(
				false,
				candidates,
				"AI가 벡터 검색 후보 중 실제 상품과 일치하는 HSK 품목을 확정하지 못했습니다."
			);
		}
		return new HskMatchResponse(
			true,
			candidates,
			"공식 관세청 HSK 품목 데이터에서 후보를 찾았습니다."
		);
	}

	private HskCandidateResponse applyRerankResult(HskCandidateResponse candidate, HskRerankResult rerankResult) {
		if (rerankResult == null || !rerankResult.selected()) {
			return candidate;
		}
		if (!candidate.hskCode().equals(rerankResult.selectedHskCode())) {
			return candidate;
		}
		return new HskCandidateResponse(
			candidate.hskCode(),
			candidate.koreanName(),
			candidate.englishName(),
			candidate.displayName(),
			Math.max(candidate.confidence(), Math.min(0.99, rerankResult.confidence())),
			firstNonBlank(rerankResult.reason(), candidate.reason())
		);
	}

	private static List<String> candidateSearchTerms(HskMatchRequest request, HskFeatures features) {
		Set<String> terms = new LinkedHashSet<>();
		addDomainTerms(terms, request, features);
		addTerms(terms, features.primaryProductName());
		if (features.primarySearchKeywords() != null) {
			features.primarySearchKeywords().forEach(keyword -> addTerms(terms, keyword));
		}
		if (request.hskCandidateKeywords() != null) {
			request.hskCandidateKeywords().forEach(keyword -> addTerms(terms, keyword));
		}
		return terms.stream()
			.filter(HskMatchService::isSearchableTerm)
			.limit(20)
			.toList();
	}

	private static void addDomainTerms(Set<String> terms, HskMatchRequest request, HskFeatures features) {
		String source = normalize(String.join(" ",
			nullToEmpty(request.productName()),
			nullToEmpty(request.description()),
			nullToEmpty(features.standardProductName()),
			nullToEmpty(features.primaryProductName()),
			features.primarySearchKeywords() == null ? "" : String.join(" ", features.primarySearchKeywords()),
			features.searchKeywords() == null ? "" : String.join(" ", features.searchKeywords())
		));

		if (containsAny(source, "노트북", "랩탑", "laptop", "notebook pc", "notebook computer")) {
			terms.add("휴대용 자동자료처리기계");
			terms.add("portable automatic data processing machine");
		}
		if (containsAny(source, "가방", "백팩", "bag", "backpack")) {
			terms.add("가방");
			terms.add("bag");
		}
		if (containsAny(source, "손목시계", "벽시계", "시계식", "시계", "wrist watch", "wrist-watch", "watch", "clock")) {
			terms.add("손목시계");
			terms.add("시계");
			terms.add("watch");
			terms.add("clock");
		}
	}

	private static void addTerms(Set<String> terms, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		String normalized = normalize(value);
		terms.add(normalized);
		addEquivalentTerms(terms, normalized);
		for (String token : normalized.split("\\s+")) {
			if (token.length() >= 2) {
				terms.add(token);
				addEquivalentTerms(terms, token);
			}
		}
	}

	private static void addEquivalentTerms(Set<String> terms, String value) {
		if (value.contains("손목시계") || value.contains("벽시계") || value.contains("시계식")
			|| value.equals("시계") || value.contains("wrist watch") || value.contains("wrist-watch")
			|| value.equals("watch") || value.equals("clock")) {
			terms.add("시계");
			terms.add("watch");
			terms.add("clock");
		}
	}

	private static boolean isSearchableTerm(String term) {
		if (term.length() < 2) {
			return false;
		}
		return !Set.of(
			"for", "of", "and", "the", "with",
			"제품", "상품", "기타", "일반", "용품",
			"어린이", "어린이용", "유아", "아동",
			"baby", "children", "kids"
		).contains(term);
	}

	private static boolean containsAny(String source, String... candidates) {
		for (String candidate : candidates) {
			if (source.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	private HskCandidateResponse toCandidate(
		HskVectorCandidate candidate,
		HskMatchRequest request,
		HskFeatures features
	) {
		HskItem item = candidate.item();
		String names = normalize(item.getKoreanName() + " " + item.getEnglishName());
		List<String> evidence = new ArrayList<>();
		double score = 0.25 + Math.max(0.0, Math.min(1.0, candidate.similarity())) * 0.55;
		for (String term : candidateSearchTerms(request, features)) {
			if (names.equals(term)) {
				score += 0.30;
				evidence.add(term);
			} else if (names.contains(term)) {
				score += Math.min(0.12, 0.02 + term.length() * 0.005);
				evidence.add(term);
			}
		}
		for (String component : safeList(features.componentKeywords())) {
			String normalizedComponent = normalize(component);
			if (normalizedComponent.length() >= 2 && names.contains(normalizedComponent)) {
				score += 0.01;
			}
		}
		if ("FINISHED_PRODUCT".equals(features.productForm()) && isPartOrManufacturingUse(item)) {
			score -= 0.12;
		}
		double confidence = Math.max(0.0, Math.min(0.99, Math.round(score * 100.0) / 100.0));
		String reason = evidence.isEmpty()
			? "공식 HSK 품목명과 의미 기반으로 관련된 후보입니다."
			: "공식 HSK 품목명에서 일치한 특징: " + String.join(", ", evidence.stream().distinct().limit(4).toList());
		return new HskCandidateResponse(
			item.getHskCode(),
			item.getKoreanName(),
			item.getEnglishName(),
			item.getDisplayName(),
			confidence,
			reason
		);
	}

	private boolean isPartOrManufacturingUse(HskItem item) {
		String names = normalize(item.getKoreanName() + " " + item.getEnglishName());
		return names.contains("제조용")
			|| names.contains("제조에")
			|| names.contains("부분품")
			|| names.contains("교체용")
			|| names.contains("for the manufacture")
			|| names.contains("parts of")
			|| names.endsWith("parts");
	}

	private static List<String> safeList(List<String> values) {
		return values == null ? List.of() : values;
	}

	private static String normalize(String value) {
		return nullToEmpty(value).toLowerCase(Locale.ROOT)
			.replaceAll("[^0-9a-z가-힣]+", " ")
			.trim()
			.replaceAll("\\s+", " ");
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private String firstNonBlank(String primary, String fallback) {
		return primary == null || primary.isBlank() ? fallback : primary;
	}
}
