package com.example.gonggong.domain.analysis.service;

import com.example.gonggong.domain.analysis.RiskLevel;
import com.example.gonggong.domain.analysis.dto.MatchedRecallDto;
import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import com.example.gonggong.domain.analysis.dto.ProductAnalyzeResponse;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;
import com.example.gonggong.domain.analysis.recall.HarmfulIngredientExtractor;
import com.example.gonggong.domain.analysis.recall.RecallProductNameClassifier;
import com.example.gonggong.domain.analysis.recall.RecallSource;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallClient;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallItem;
import com.example.gonggong.global.logging.DomPayloadLogFormatter;
import com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class ProductAnalyzeService {

	private static final Logger log = LoggerFactory.getLogger(ProductAnalyzeService.class);
	private static final DateTimeFormatter SAFETY_KOREA_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter RESPONSE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final int MAX_RECALL_CANDIDATES = 30;
	private static final int MAX_RECALL_CANDIDATES_FOR_AI = 8;
	private static final int MAX_RECALL_CANDIDATE_LOG_ITEMS = 5;

	private final ProductNormalizer productNormalizer;
	private final SafetyKoreaRecallClient recallClient;
	private final RecallProductNameClassifier classifier;
	private final HarmfulIngredientExtractor ingredientExtractor;
	private final RecallRelevanceDecider recallRelevanceDecider;

	@Autowired
	public ProductAnalyzeService(
		ProductNormalizer productNormalizer,
		SafetyKoreaRecallClient recallClient,
		RecallProductNameClassifier classifier,
		HarmfulIngredientExtractor ingredientExtractor,
		RecallRelevanceDecider recallRelevanceDecider
	) {
		this.productNormalizer = productNormalizer;
		this.recallClient = recallClient;
		this.classifier = classifier;
		this.ingredientExtractor = ingredientExtractor;
		this.recallRelevanceDecider = recallRelevanceDecider;
	}

	public ProductAnalyzeService(
		ProductNormalizer productNormalizer,
		SafetyKoreaRecallClient recallClient,
		RecallProductNameClassifier classifier,
		HarmfulIngredientExtractor ingredientExtractor
	) {
		this(
			productNormalizer,
			recallClient,
			classifier,
			ingredientExtractor,
			RecallRelevanceDecider.keepAll()
		);
	}

	public ProductAnalyzeResponse analyze(ProductAnalyzeRequest request) {
		logDomAnalyzeRequest(request);
		ProductNormalizeResult normalized = productNormalizer.normalize(request);
		logNormalizedProduct(normalized);
		List<SafetyKoreaRecallItem> recallCandidates = enrichDetails(findRecalls(normalized));
		logRecallCandidates("SafetyKorea recall candidates", recallCandidates);
		List<SafetyKoreaRecallItem> recalls = selectRelevantRecalls(normalized, recallCandidates);
		logRecallCandidates("OpenAI-filtered recall candidates", recalls);
		List<ChemicalIngredientCandidate> chemicalCandidates = resolveChemicalCandidates(normalized);
		List<String> harmfulIngredients = resolveHarmfulIngredients(chemicalCandidates, recalls);

		if (recalls.isEmpty()) {
			return new ProductAnalyzeResponse(
				RiskLevel.NORMAL,
				15,
				fallbackCategory(normalized, null),
				normalized.brandName(),
				normalized.modelName(),
				firstNonBlank(normalized.certNum(), request.kcCertificationNumber()),
				normalized.standardProductName(),
				safeList(normalized.hskCandidateKeywords()),
				normalized.primaryProductName(),
				normalized.productForm(),
				safeList(normalized.primarySearchKeywords()),
				safeList(normalized.kcCertificationSearchKeywords()),
				safeList(normalized.componentKeywords()),
				safeList(normalized.featureKeywords()),
				"매칭된 리콜 이력이 없습니다.",
				harmfulIngredients,
				chemicalCandidates,
				List.of(),
				"현재 검색 조건에서 유사 리콜 이력이 확인되지 않았습니다."
			);
		}

		List<MatchedRecallDto> matchedRecalls = recalls.stream()
			.sorted(Comparator.comparing(SafetyKoreaRecallItem::publishDate, Comparator.nullsLast(Comparator.reverseOrder())))
			.map(item -> new MatchedRecallDto(
				item.recallProductName(),
				item.recallModelName(),
				item.recallCmpnyName(),
				recallReason(item),
				formatDate(item.publishDate()),
				similarity(normalized, item),
				item.source().name()
			))
			.toList();
		SafetyKoreaRecallItem topRecall = recalls.get(0);
		RiskLevel riskLevel = riskLevel(normalized, recalls, harmfulIngredients);
		int riskScore = riskScore(riskLevel, matchedRecalls);

		return new ProductAnalyzeResponse(
			riskLevel,
			riskScore,
			fallbackCategory(normalized, topRecall),
			normalized.brandName(),
			normalized.modelName(),
			firstNonBlank(normalized.certNum(), request.kcCertificationNumber()),
			normalized.standardProductName(),
			safeList(normalized.hskCandidateKeywords()),
			normalized.primaryProductName(),
			normalized.productForm(),
			safeList(normalized.primarySearchKeywords()),
			safeList(normalized.kcCertificationSearchKeywords()),
			safeList(normalized.componentKeywords()),
			safeList(normalized.featureKeywords()),
			recallReason(topRecall),
			harmfulIngredients,
			chemicalCandidates,
			matchedRecalls,
			"유사 리콜 이력이 있는 상품입니다. 구매 전 상세 정보를 확인하세요."
		);
	}

	private List<SafetyKoreaRecallItem> findRecalls(ProductNormalizeResult normalized) {
		Map<String, SafetyKoreaRecallItem> deduplicated = new LinkedHashMap<>();
		for (String keyword : searchKeywords(normalized)) {
			for (SafetyKoreaRecallItem item : safeSearch("domestic-product", keyword, () -> recallClient.searchByProductName(keyword))) {
				SafetyKoreaRecallItem matchedItem = item.withMatchedQuery(keyword);
				if (isRelevantRecall(normalized, matchedItem)) {
					deduplicated.putIfAbsent(recallKey(matchedItem), matchedItem);
					if (deduplicated.size() >= MAX_RECALL_CANDIDATES) {
						return recallCandidates(deduplicated);
					}
				}
			}
			for (SafetyKoreaRecallItem item : safeSearch("foreign-product", keyword, () -> recallClient.searchForeignByProductName(keyword))) {
				SafetyKoreaRecallItem foreignItem = item.withSource(RecallSource.FOREIGN).withMatchedQuery(keyword);
				if (isRelevantRecall(normalized, foreignItem)) {
					deduplicated.putIfAbsent(recallKey(foreignItem), foreignItem);
					if (deduplicated.size() >= MAX_RECALL_CANDIDATES) {
						return recallCandidates(deduplicated);
					}
				}
			}
		}

		if (normalized.brandName() != null && !normalized.brandName().isBlank()) {
			for (SafetyKoreaRecallItem item : safeSearch("domestic-brand", normalized.brandName(), () -> recallClient.searchByBrandName(normalized.brandName()))) {
				if (isRelevantRecall(normalized, item)) {
					deduplicated.putIfAbsent(recallKey(item), item);
					if (deduplicated.size() >= MAX_RECALL_CANDIDATES) {
						return recallCandidates(deduplicated);
					}
				}
			}
			for (SafetyKoreaRecallItem item : safeSearch("foreign-brand", normalized.brandName(), () -> recallClient.searchForeignByBrandName(normalized.brandName()))) {
				SafetyKoreaRecallItem foreignItem = item.withSource(RecallSource.FOREIGN);
				if (isRelevantRecall(normalized, foreignItem)) {
					deduplicated.putIfAbsent(recallKey(foreignItem), foreignItem);
					if (deduplicated.size() >= MAX_RECALL_CANDIDATES) {
						return recallCandidates(deduplicated);
					}
				}
			}
		}

		return recallCandidates(deduplicated);
	}

	private List<SafetyKoreaRecallItem> recallCandidates(Map<String, SafetyKoreaRecallItem> deduplicated) {
		return new ArrayList<>(deduplicated.values()).stream()
			.limit(MAX_RECALL_CANDIDATES)
			.toList();
	}

	private List<SafetyKoreaRecallItem> selectRelevantRecalls(
		ProductNormalizeResult normalized,
		List<SafetyKoreaRecallItem> recallCandidates
	) {
		if (recallCandidates == null || recallCandidates.isEmpty()) {
			log.info("OpenAI recall relevance skipped reason=no-recall-candidates");
			return List.of();
		}
		List<SafetyKoreaRecallItem> limitedCandidates = recallCandidates.stream()
			.limit(MAX_RECALL_CANDIDATES_FOR_AI)
			.toList();
		try {
			log.info(
				"OpenAI recall relevance requested candidateCount={} limitedCandidateCount={}",
				recallCandidates.size(),
				limitedCandidates.size()
			);
			return recallRelevanceDecider.selectRelevant(normalized, limitedCandidates);
		} catch (AnalysisException exception) {
			log.warn("Recall relevance decision skipped code={}", exception.getBaseCode().getCode());
			return limitedCandidates;
		}
	}

	private void logDomAnalyzeRequest(ProductAnalyzeRequest request) {
		log.info(
			"DOM product payload received endpoint=product-analyze productName={} description={} site={} pageUrl={} imageUrl={} sellerName={} kcCertificationNumber={} kcCertificationType={}",
			DomPayloadLogFormatter.clip(request.productName(), 120),
			DomPayloadLogFormatter.clip(request.description(), 220),
			request.site(),
			DomPayloadLogFormatter.clip(request.pageUrl(), 180),
			DomPayloadLogFormatter.clip(request.imageUrl(), 180),
			DomPayloadLogFormatter.clip(request.sellerName(), 80),
			DomPayloadLogFormatter.maskCertificationNumber(request.kcCertificationNumber()),
			DomPayloadLogFormatter.clip(request.kcCertificationType(), 80)
		);
	}

	private void logNormalizedProduct(ProductNormalizeResult normalized) {
		log.info(
			"OpenAI product normalization result standardProductName={} primaryProductName={} productForm={} brandName={} category={} matchedRecallProductName={} modelName={} searchKeywords={} primarySearchKeywords={} kcCertificationSearchKeywords={} hskCandidateKeywords={} riskIngredientKeywords={} confidence={}",
			normalized.standardProductName(),
			normalized.primaryProductName(),
			normalized.productForm(),
			normalized.brandName(),
			normalized.category(),
			normalized.matchedRecallProductName(),
			normalized.modelName(),
			safeList(normalized.searchKeywords()),
			safeList(normalized.primarySearchKeywords()),
			safeList(normalized.kcCertificationSearchKeywords()),
			safeList(normalized.hskCandidateKeywords()),
			safeList(normalized.riskIngredientKeywords()),
			normalized.confidence()
		);
	}

	private void logRecallCandidates(String label, List<SafetyKoreaRecallItem> candidates) {
		List<SafetyKoreaRecallItem> safeCandidates = candidates == null ? List.of() : candidates;
		log.info("{} count={}", label, safeCandidates.size());
		int logCount = Math.min(safeCandidates.size(), MAX_RECALL_CANDIDATE_LOG_ITEMS);
		for (int index = 0; index < logCount; index += 1) {
			SafetyKoreaRecallItem item = safeCandidates.get(index);
			log.info(
				"{} itemIndex={} source={} recallUid={} recallProductName={} recallBrandName={} recallModelName={} publishDate={} matchedQuery={} harmDscr={} accidentCaseDscr={}",
				label,
				index,
				item.source(),
				item.recallUid(),
				item.recallProductName(),
				item.recallBrandName(),
				item.recallModelName(),
				item.publishDate(),
				item.matchedQuery(),
				truncate(item.harmDscr()),
				truncate(item.accidentCaseDscr())
			);
		}
		if (safeCandidates.size() > logCount) {
			log.info("{} detailLogTruncated loggedCount={} skippedCount={}", label, logCount, safeCandidates.size() - logCount);
		}
	}

	private boolean isRelevantRecall(ProductNormalizeResult normalized, SafetyKoreaRecallItem item) {
		String target = normalizeForMatch(String.join(" ",
			nullToEmpty(normalized.standardProductName()),
			nullToEmpty(normalized.primaryProductName()),
			normalized.primarySearchKeywords() == null ? "" : String.join(" ", normalized.primarySearchKeywords()),
			normalized.searchKeywords() == null ? "" : String.join(" ", normalized.searchKeywords())
		));
		String recallProductName = normalizeForMatch(item.recallProductName());

		if (hasShapeOnlyConflict(target, recallProductName)) {
			return false;
		}
		return true;
	}

	private boolean hasShapeOnlyConflict(String target, String recallProductName) {
		int shapeIndex = firstShapeMarkerIndex(recallProductName);
		if (shapeIndex < 0) {
			return false;
		}
		int shapeMarkerStartIndex = firstShapeMarkerStartIndex(recallProductName);
		String descriptor = recallProductName.substring(0, Math.max(0, shapeMarkerStartIndex)).trim();
		String subject = recallProductName.substring(shapeIndex);
		if (isShapeDescriptorOnlyMatch(target, descriptor, subject)) {
			return true;
		}
		return containsAny(subject, "손난로", "전기손난로", "난로", "히터", "온열", "찜질")
			&& !containsAny(target, "손난로", "전기손난로", "난로", "히터", "온열", "찜질");
	}

	private boolean isShapeDescriptorOnlyMatch(String target, String descriptor, String subject) {
		String normalizedDescriptor = normalizeForMatch(descriptor);
		String normalizedSubject = normalizeForMatch(subject);
		if (normalizedDescriptor.length() < 2 || normalizedSubject.length() < 2) {
			return false;
		}
		return target.contains(normalizedDescriptor) && !target.contains(normalizedSubject);
	}

	private int firstShapeMarkerIndex(String value) {
		int result = -1;
		for (String marker : List.of("모양", "형태", "디자인")) {
			int index = value.indexOf(marker);
			if (index >= 0 && (result < 0 || index < result)) {
				result = index + marker.length();
			}
		}
		return result;
	}

	private boolean containsAny(String value, String... keywords) {
		for (String keyword : keywords) {
			if (value.contains(normalizeForMatch(keyword))) {
				return true;
			}
		}
		return false;
	}

	private int firstShapeMarkerStartIndex(String value) {
		int result = -1;
		for (String marker : List.of("모양", "형태", "디자인")) {
			int index = value.indexOf(marker);
			if (index >= 0 && (result < 0 || index < result)) {
				result = index;
			}
		}
		return result;
	}

	private List<SafetyKoreaRecallItem> safeSearch(String scope, String query, RecallSearch search) {
		try {
			return search.run();
		} catch (AnalysisException exception) {
			log.warn("SafetyKorea recall search skipped scope={} query={} code={}", scope, query, exception.getBaseCode().getCode());
			return List.of();
		}
	}

	private List<SafetyKoreaRecallItem> enrichDetails(List<SafetyKoreaRecallItem> recalls) {
		List<SafetyKoreaRecallItem> enriched = new ArrayList<>();
		for (SafetyKoreaRecallItem item : recalls) {
			if (needsDetail(item)) {
				SafetyKoreaRecallItem detail = recallClient.findDetail(item.recallUid());
				enriched.add(merge(item, detail));
			} else {
				enriched.add(item);
			}
		}
		return enriched;
	}

	private boolean needsDetail(SafetyKoreaRecallItem item) {
		return item.source() == RecallSource.DOMESTIC
			&& item.recallUid() != null && !item.recallUid().isBlank()
			&& (item.harmDscr() == null || item.harmDscr().isBlank())
			&& (item.accidentCaseDscr() == null || item.accidentCaseDscr().isBlank());
	}

	private SafetyKoreaRecallItem merge(SafetyKoreaRecallItem listItem, SafetyKoreaRecallItem detailItem) {
		if (detailItem == null) {
			return listItem;
		}
		return new SafetyKoreaRecallItem(
			firstNonBlank(detailItem.recallUid(), listItem.recallUid()),
			firstNonBlank(detailItem.recallProductName(), listItem.recallProductName()),
			firstNonBlank(detailItem.recallBrandName(), listItem.recallBrandName()),
			firstNonBlank(detailItem.recallModelName(), listItem.recallModelName()),
			firstNonBlank(detailItem.recallCmpnyName(), listItem.recallCmpnyName()),
			firstNonBlank(detailItem.publishDate(), listItem.publishDate()),
			firstNonBlank(detailItem.recallStaDate(), listItem.recallStaDate()),
			firstNonBlank(detailItem.recallEndDate(), listItem.recallEndDate()),
			firstNonBlank(detailItem.barcodeNum(), listItem.barcodeNum()),
			firstNonBlank(detailItem.certNum(), listItem.certNum()),
			firstNonBlank(detailItem.harmDscr(), listItem.harmDscr()),
			firstNonBlank(detailItem.accidentCaseDscr(), listItem.accidentCaseDscr()),
			firstNonBlank(detailItem.publishActionDscr(), listItem.publishActionDscr()),
			detailItem.imageUrls() == null || detailItem.imageUrls().isEmpty() ? listItem.imageUrls() : detailItem.imageUrls(),
			firstNonBlank(detailItem.matchedQuery(), listItem.matchedQuery()),
			listItem.source(),
			firstNonBlank(detailItem.sourceUrl(), listItem.sourceUrl())
		);
	}

	private List<String> searchKeywords(ProductNormalizeResult normalized) {
		List<String> keywords = new ArrayList<>();
		if (normalized.searchKeywords() != null) {
			keywords.addAll(normalized.searchKeywords());
		}
		return keywords.stream()
			.filter(keyword -> keyword != null && !keyword.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
	}

	private String recallKey(SafetyKoreaRecallItem item) {
		String source = item.source() == null ? RecallSource.DOMESTIC.name() : item.source().name();
		if (item.recallUid() != null && !item.recallUid().isBlank()) {
			return source + "|" + item.recallUid();
		}
		return source + "|" + String.join("|",
			nullToEmpty(item.recallProductName()),
			nullToEmpty(item.recallBrandName()),
			nullToEmpty(item.recallModelName())
		);
	}

	private String fallbackCategory(ProductNormalizeResult normalized, SafetyKoreaRecallItem item) {
		if (normalized.category() != null && !normalized.category().isBlank()) {
			return normalized.category();
		}
		if (item != null) {
			return classifier.classify(item.recallProductName());
		}
		return "기타";
	}

	private String recallReason(SafetyKoreaRecallItem item) {
		if (item.harmDscr() != null && !item.harmDscr().isBlank()) {
			return item.harmDscr();
		}
		if (item.accidentCaseDscr() != null && !item.accidentCaseDscr().isBlank()) {
			return item.accidentCaseDscr();
		}
		if (item.publishActionDscr() != null && !item.publishActionDscr().isBlank()) {
			return item.publishActionDscr();
		}
		return "리콜 상세 사유가 제공되지 않았습니다.";
	}

	private List<String> collectIngredients(List<SafetyKoreaRecallItem> recalls) {
		return recalls.stream()
			.flatMap(item -> ingredientExtractor.extract(recallReason(item)).stream())
			.distinct()
			.toList();
	}

	private List<ChemicalIngredientCandidate> resolveChemicalCandidates(ProductNormalizeResult normalized) {
		List<ChemicalIngredientCandidate> candidates = normalized.chemicalCandidates();
		if (!candidates.isEmpty()) {
			return candidates;
		}
		return safeList(normalized.riskIngredientKeywords()).stream()
			.distinct()
			.map(name -> new ChemicalIngredientCandidate(name, null, null))
			.toList();
	}

	private List<String> resolveHarmfulIngredients(List<ChemicalIngredientCandidate> chemicalCandidates, List<SafetyKoreaRecallItem> recalls) {
		List<String> aiRefinedIngredients = chemicalCandidates.stream()
			.map(ChemicalIngredientCandidate::name)
			.filter(name -> name != null && !name.isBlank())
			.distinct()
			.toList();
		if (!aiRefinedIngredients.isEmpty()) {
			return aiRefinedIngredients;
		}
		return collectIngredients(recalls);
	}

	private double similarity(ProductNormalizeResult normalized, SafetyKoreaRecallItem item) {
		MatchStrength strength = matchStrength(normalized, item);
		double score = Math.max(0.0, normalized.confidence());
		if (strength == MatchStrength.EXACT) {
			score = Math.max(score, 0.95);
		} else if (strength == MatchStrength.STRONG) {
			score = Math.max(score, 0.86);
		} else if (strength == MatchStrength.CATEGORY) {
			score = Math.min(score, 0.79);
		}
		return Math.min(1.0, score);
	}

	private String formatDate(String date) {
		if (date == null || date.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(date, SAFETY_KOREA_DATE).format(RESPONSE_DATE);
		} catch (DateTimeParseException exception) {
			return date;
		}
	}

	private RiskLevel riskLevel(
		ProductNormalizeResult normalized,
		List<SafetyKoreaRecallItem> recalls,
		List<String> harmfulIngredients
	) {
		MatchStrength strongest = recalls.stream()
			.map(item -> matchStrength(normalized, item))
			.max(Comparator.comparingInt(MatchStrength::rank))
			.orElse(MatchStrength.NONE);
		boolean hasHarmfulIngredient = !harmfulIngredients.isEmpty();
		boolean hasCompletedAction = recalls.stream().anyMatch(this::hasCompletedAction);

		if ((strongest == MatchStrength.EXACT || strongest == MatchStrength.STRONG) && hasHarmfulIngredient) {
			return RiskLevel.DANGER;
		}
		if (strongest == MatchStrength.EXACT) {
			return RiskLevel.DANGER;
		}
		if (hasConcreteSevereHazardMatch(recalls, harmfulIngredients)) {
			return RiskLevel.DANGER;
		}
		if (strongest == MatchStrength.STRONG || hasHarmfulIngredient || recalls.stream().anyMatch(this::hasPhysicalOrStandardDefect)) {
			return RiskLevel.WARNING;
		}
		if (hasCompletedAction || strongest == MatchStrength.CATEGORY) {
			return RiskLevel.REVIEW;
		}
		return RiskLevel.NORMAL;
	}

	private boolean hasConcreteSevereHazardMatch(List<SafetyKoreaRecallItem> recalls, List<String> harmfulIngredients) {
		return hasSevereHazard(harmfulIngredients) && recalls.stream()
			.map(SafetyKoreaRecallItem::matchedQuery)
			.anyMatch(this::isConcreteProductQuery);
	}

	private boolean hasSevereHazard(List<String> harmfulIngredients) {
		return harmfulIngredients.stream().anyMatch(ingredient ->
			ingredient.equals("프탈레이트계 가소제")
				|| ingredient.equals("DEHP")
				|| ingredient.equals("DBP")
				|| ingredient.equals("DINP")
				|| ingredient.equals("DIBP")
				|| ingredient.equals("납")
				|| ingredient.equals("카드뮴")
				|| ingredient.equals("폼알데하이드")
				|| ingredient.equals("비스페놀A")
		);
	}

	private boolean isConcreteProductQuery(String query) {
		if (query == null || query.isBlank()) {
			return false;
		}
		String normalized = normalizeForMatch(query);
		if (normalized.length() < 5) {
			return false;
		}
		return !List.of(
			"완구",
			"장난감",
			"책가방",
			"식기",
			"유아용품",
			"아동용품",
			"가정용섬유제품",
			"운동완구",
			"생활용품",
			"주방용품"
		).contains(normalized);
	}

	private int riskScore(RiskLevel riskLevel, List<MatchedRecallDto> matchedRecalls) {
		double maxSimilarity = matchedRecalls.stream()
			.mapToDouble(MatchedRecallDto::similarity)
			.max()
			.orElse(0.0);
		int similarityScore = (int)Math.round(maxSimilarity * 100);
		return switch (riskLevel) {
			case DANGER -> Math.max(90, similarityScore);
			case WARNING -> Math.min(79, Math.max(60, similarityScore));
			case REVIEW -> Math.min(59, Math.max(30, similarityScore));
			case NORMAL -> Math.min(29, similarityScore);
		};
	}

	private MatchStrength matchStrength(ProductNormalizeResult normalized, SafetyKoreaRecallItem item) {
		if (equalsIgnoreBlank(normalized.barcodeNum(), item.barcodeNum())
			|| equalsIgnoreBlank(normalized.certNum(), item.certNum())
			|| equalsIgnoreBlank(normalized.modelName(), item.recallModelName())) {
			return MatchStrength.EXACT;
		}

		boolean brandMatches = containsIgnoreBlank(item.recallBrandName(), normalized.brandName())
			|| containsIgnoreBlank(normalized.brandName(), item.recallBrandName());
		boolean modelSimilar = containsIgnoreBlank(item.recallModelName(), normalized.modelName())
			|| containsIgnoreBlank(normalized.modelName(), item.recallModelName());
		boolean productNameMatches = equalsIgnoreBlank(normalized.matchedRecallProductName(), item.recallProductName());

		if (brandMatches && (modelSimilar || productNameMatches)) {
			return MatchStrength.STRONG;
		}
		if (productNameMatches || fallbackCategory(normalized, item).equals(classifier.classify(item.recallProductName()))) {
			return MatchStrength.CATEGORY;
		}
		return MatchStrength.NONE;
	}

	private boolean hasCompletedAction(SafetyKoreaRecallItem item) {
		String text = String.join(" ",
			nullToEmpty(item.recallEndDate()),
			nullToEmpty(item.publishActionDscr())
		);
		return !text.isBlank() && (text.contains("완료") || text.contains("환불") || text.contains("교환") || text.contains("수거"));
	}

	private boolean hasPhysicalOrStandardDefect(SafetyKoreaRecallItem item) {
		String reason = recallReason(item);
		return reason.contains("규격") || reason.contains("부적합") || reason.contains("표시사항")
			|| reason.contains("물리") || reason.contains("파손") || reason.contains("질식") || reason.contains("상해");
	}

	private boolean equalsIgnoreBlank(String left, String right) {
		return left != null && right != null
			&& !left.isBlank() && !right.isBlank()
			&& normalizeForMatch(left).equals(normalizeForMatch(right));
	}

	private boolean containsIgnoreBlank(String container, String value) {
		return container != null && value != null
			&& !container.isBlank() && !value.isBlank()
			&& normalizeForMatch(container).contains(normalizeForMatch(value));
	}

	private String normalizeForMatch(String value) {
		return value.replaceAll("[\\s\\-_/()]", "").toLowerCase();
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private String truncate(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		String trimmed = value.trim();
		if (trimmed.length() <= 180) {
			return trimmed;
		}
		return trimmed.substring(0, 180) + "...";
	}

	private String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		return fallback;
	}

	private List<String> safeList(List<String> values) {
		return values == null ? List.of() : values;
	}

	private enum MatchStrength {
		NONE(0),
		CATEGORY(1),
		STRONG(2),
		EXACT(3);

		private final int rank;

		MatchStrength(int rank) {
			this.rank = rank;
		}

		private int rank() {
			return rank;
		}
	}

	@FunctionalInterface
	private interface RecallSearch {
		List<SafetyKoreaRecallItem> run();
	}
}
