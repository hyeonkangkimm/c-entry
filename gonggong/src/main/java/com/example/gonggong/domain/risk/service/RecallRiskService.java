package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;
import com.example.gonggong.domain.analysis.recall.RecallSource;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallClient;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallItem;
import com.example.gonggong.domain.analysis.service.RecallRelevanceDecider;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.dto.request.PrefilteredRecallRequest;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.RecallRiskItemResponse;
import com.example.gonggong.domain.risk.dto.response.RecallRiskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecallRiskService {

	private static final Logger log = LoggerFactory.getLogger(RecallRiskService.class);
	private static final DateTimeFormatter SAFETY_KOREA_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter RESPONSE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final String DEFAULT_REASON = "사유 미명시(단순 안전기준 미달 적발)";
	private static final String NO_RECALL_MESSAGE = "최근 3개년 내 동종 품목의 국외 리콜 이력이 발견되지 않은 품목입니다. 리콜 이력이 없다는 것이 절대적인 안전을 의미하지는 않습니다.";

	private final SafetyKoreaRecallClient recallClient;
	private final RecallRelevanceDecider recallRelevanceDecider;
	private final Clock clock;

	@Autowired
	public RecallRiskService(SafetyKoreaRecallClient recallClient, RecallRelevanceDecider recallRelevanceDecider) {
		this(recallClient, Clock.systemDefaultZone(), recallRelevanceDecider);
	}

	RecallRiskService(SafetyKoreaRecallClient recallClient, Clock clock) {
		this(recallClient, clock, RecallRelevanceDecider.keepAll());
	}

	RecallRiskService(SafetyKoreaRecallClient recallClient, Clock clock, RecallRelevanceDecider recallRelevanceDecider) {
		this.recallClient = recallClient;
		this.recallRelevanceDecider = recallRelevanceDecider == null ? RecallRelevanceDecider.keepAll() : recallRelevanceDecider;
		this.clock = clock;
	}

	public RecallRiskResponse analyze(RiskDashboardAnalyzeRequest request) {
		List<PrefilteredRecallRequest> prefilteredRecalls = request.normalizedPrefilteredRecalls();
		if (!prefilteredRecalls.isEmpty()) {
			return fromPrefilteredRecalls(prefilteredRecalls);
		}

		Map<String, SafetyKoreaRecallItem> recalls = new LinkedHashMap<>();
		boolean anySearchSucceeded = false;

		for (String keyword : recallKeywords(request)) {
			SearchResult domestic = safeSearch("domestic", keyword, () -> recallClient.searchByProductName(keyword));
			anySearchSucceeded = anySearchSucceeded || domestic.succeeded();
			addAll(recalls, domestic.items(), RecallSource.DOMESTIC);

			SearchResult foreign = safeSearch("foreign", keyword, () -> recallClient.searchForeignByProductName(keyword));
			anySearchSucceeded = anySearchSucceeded || foreign.succeeded();
			addAll(recalls, foreign.items(), RecallSource.FOREIGN);
		}

		if (!anySearchSucceeded) {
			return new RecallRiskResponse(
				RiskStatus.UNAVAILABLE,
				0,
				0,
				null,
				"리콜 정보를 현재 조회할 수 없습니다.",
				List.of()
			);
		}

		List<SafetyKoreaRecallItem> relevantRecalls = selectRelevantRecalls(request, new ArrayList<>(recalls.values()));
		List<RecallRiskItemResponse> items = relevantRecalls.stream()
			.filter(this::isWithinRecentThreeYears)
			.sorted(Comparator.comparing(this::publishedDate, Comparator.nullsLast(Comparator.reverseOrder())))
			.map(this::toResponse)
			.toList();

		if (items.isEmpty()) {
			return new RecallRiskResponse(RiskStatus.SAFE, 15, 0, null, NO_RECALL_MESSAGE, List.of());
		}

		int score = Math.min(100, 50 + items.size() * 5);
		return new RecallRiskResponse(
			items.size() >= 5 ? RiskStatus.DANGER : RiskStatus.WARNING,
			score,
			items.size(),
			items.get(0).announcementDate(),
			"최근 3개년 동안 동종 품목 리콜 이력이 확인되었습니다.",
			items
		);
	}

	private List<String> recallKeywords(RiskDashboardAnalyzeRequest request) {
		return request.normalizedRecallKeywords();
	}

	private RecallRiskResponse fromPrefilteredRecalls(List<PrefilteredRecallRequest> prefilteredRecalls) {
		List<RecallRiskItemResponse> items = prefilteredRecalls.stream()
			.map(this::toResponse)
			.toList();
		int score = Math.min(100, 50 + items.size() * 5);
		String latestAnnouncementDate = items.stream()
			.map(RecallRiskItemResponse::announcementDate)
			.filter(date -> date != null && !date.isBlank())
			.max(String::compareTo)
			.orElse(null);
		return new RecallRiskResponse(
			items.size() >= 5 ? RiskStatus.DANGER : RiskStatus.WARNING,
			score,
			items.size(),
			latestAnnouncementDate,
			"이미 분석된 관련 리콜 이력을 기준으로 리콜 가능성을 계산했습니다.",
			items
		);
	}

	private List<SafetyKoreaRecallItem> selectRelevantRecalls(RiskDashboardAnalyzeRequest request, List<SafetyKoreaRecallItem> candidates) {
		if (candidates.isEmpty()) {
			return List.of();
		}
		ProductNormalizeResult normalized = new ProductNormalizeResult(
			firstNonBlank(request.standardProductName(), request.primaryProductName(), request.productName()),
			request.normalizedRecallKeywords(),
			null,
			null,
			firstNonBlank(request.primaryProductName(), request.standardProductName(), request.productName()),
			null,
			null,
			null,
			List.of(),
			null,
			request.normalizedIngredients(),
			request.normalizedRecallKeywords(),
			firstNonBlank(request.primaryProductName(), request.standardProductName(), request.productName()),
			"UNKNOWN",
			request.normalizedRecallKeywords(),
			request.normalizedKcCertificationSearchKeywords(),
			List.of(),
			List.of(),
			0.0
		);
		try {
			List<SafetyKoreaRecallItem> selected = recallRelevanceDecider.selectRelevant(normalized, candidates);
			log.info(
				"Risk dashboard recall relevance filtered candidateCount={} selectedCount={}",
				candidates.size(),
				selected == null ? 0 : selected.size()
			);
			return selected == null ? List.of() : selected;
		} catch (AnalysisException exception) {
			log.warn(
				"Risk dashboard recall relevance skipped candidateCount={} code={}",
				candidates.size(),
				exception.getBaseCode().getCode()
			);
			return candidates;
		}
	}

	private SearchResult safeSearch(String scope, String keyword, RecallSearch search) {
		try {
			return new SearchResult(true, search.run());
		} catch (AnalysisException exception) {
			log.warn("Risk dashboard recall search skipped scope={} keyword={} code={}", scope, keyword, exception.getBaseCode().getCode());
			return new SearchResult(false, List.of());
		}
	}

	private void addAll(Map<String, SafetyKoreaRecallItem> recalls, List<SafetyKoreaRecallItem> items, RecallSource source) {
		for (SafetyKoreaRecallItem item : items) {
			SafetyKoreaRecallItem sourced = item.withSource(source);
			recalls.putIfAbsent(recallKey(sourced), sourced);
		}
	}

	private String recallKey(SafetyKoreaRecallItem item) {
		String source = item.source() == null ? RecallSource.DOMESTIC.name() : item.source().name();
		if (item.recallUid() != null && !item.recallUid().isBlank()) {
			return source + "|" + item.recallUid();
		}
		return source + "|" + nullToEmpty(item.recallProductName()) + "|" + nullToEmpty(item.recallBrandName()) + "|" + nullToEmpty(item.recallModelName());
	}

	private boolean isWithinRecentThreeYears(SafetyKoreaRecallItem item) {
		LocalDate date = publishedDate(item);
		return date == null || !date.isBefore(LocalDate.now(clock).minusYears(3));
	}

	private RecallRiskItemResponse toResponse(SafetyKoreaRecallItem item) {
		return new RecallRiskItemResponse(
			item.recallProductName(),
			blankToDefault(item.harmDscr()),
			item.accidentCaseDscr(),
			formatDate(item.publishDate()),
			item.sourceUrl(),
			item.source().name()
		);
	}

	private RecallRiskItemResponse toResponse(PrefilteredRecallRequest item) {
		return new RecallRiskItemResponse(
			item.recallProductName(),
			blankToDefault(item.reason()),
			null,
			item.announcementDate(),
			null,
			blankToDefaultSource(item.source())
		);
	}

	private LocalDate publishedDate(SafetyKoreaRecallItem item) {
		if (item.publishDate() == null || item.publishDate().isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(item.publishDate(), SAFETY_KOREA_DATE);
		} catch (DateTimeParseException exception) {
			return null;
		}
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

	private String blankToDefault(String value) {
		return value == null || value.isBlank() ? DEFAULT_REASON : value;
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private String blankToDefaultSource(String value) {
		return value == null || value.isBlank() ? RecallSource.DOMESTIC.name() : value;
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	@FunctionalInterface
	private interface RecallSearch {
		List<SafetyKoreaRecallItem> run();
	}

	private record SearchResult(boolean succeeded, List<SafetyKoreaRecallItem> items) {
	}
}
