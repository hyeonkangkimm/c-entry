package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;
import com.example.gonggong.domain.analysis.recall.RecallSource;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallClient;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallItem;
import com.example.gonggong.domain.analysis.service.RecallRelevanceDecider;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.dto.request.PrefilteredRecallRequest;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.RecallRiskResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecallRiskServiceTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneId.of("Asia/Seoul"));

	@Test
	void searchesDomesticAndForeignRecallsWithProductTerms() {
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient();
		recallClient.domesticItems = List.of(new SafetyKoreaRecallItem(
			"1",
			"children plastic tableware",
			null,
			null,
			"maker",
			"20251120",
			"phthalate limit exceeded",
			"safety standard violation",
			null,
			List.of()
		));
		recallClient.foreignItems = List.of(new SafetyKoreaRecallItem(
			"2",
			"plastic tableware",
			null,
			null,
			"foreign maker",
			"20240101",
			null,
			null,
			null,
			List.of()
		).withSource(RecallSource.FOREIGN));
		RecallRiskService service = new RecallRiskService(recallClient, clock);

		RecallRiskResponse response = service.analyze(request());

		assertThat(recallClient.domesticProductQueries).contains("baby plastic bowl", "plastic tableware for children");
		assertThat(recallClient.foreignProductQueries).contains("baby plastic bowl", "plastic tableware for children");
		assertThat(recallClient.domesticProductQueries).doesNotContain("3924100000");
		assertThat(recallClient.foreignProductQueries).doesNotContain("3924100000");
		assertThat(response.status()).isEqualTo(RiskStatus.WARNING);
		assertThat(response.totalCount()).isEqualTo(2);
		assertThat(response.latestAnnouncementDate()).isEqualTo("2025-11-20");
	}

	@Test
	void prefersNormalizedRecallKeywordsOverRawDescription() {
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient();
		RecallRiskService service = new RecallRiskService(recallClient, clock);

		service.analyze(new RiskDashboardAnalyzeRequest(
			"8471300000",
			"14.1 inch 16GB RAM 2TB SSD Intel laptop",
			"1920x1080 display office study computer",
			List.of(),
			"CN",
			new BigDecimal("100000"),
			"KRW",
			1,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			null,
			"laptop computer",
			"laptop computer",
			List.of("laptop", "notebook computer")
		));

		assertThat(recallClient.domesticProductQueries).contains("laptop", "notebook computer");
		assertThat(recallClient.foreignProductQueries).contains("laptop", "notebook computer");
		assertThat(recallClient.domesticProductQueries).doesNotContain("1920x1080 display office study computer");
	}

	@Test
	void countsOnlyRecallsSelectedBySecondAiFiltering() {
		SafetyKoreaRecallItem keyboard = new SafetyKoreaRecallItem(
			"keyboard",
			"keyboard",
			null,
			null,
			"maker",
			"20251120",
			"fire risk",
			"battery overheating",
			null,
			List.of()
		);
		SafetyKoreaRecallItem keyring = new SafetyKoreaRecallItem(
			"keyring",
			"keyboard keycap shaped keyring",
			null,
			null,
			"maker",
			"20251121",
			"small parts risk",
			"toy accessory",
			null,
			List.of()
		);
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient();
		recallClient.domesticItems = List.of(keyboard, keyring);
		FakeRecallRelevanceDecider relevanceDecider = new FakeRecallRelevanceDecider(List.of(keyboard));
		RecallRiskService service = new RecallRiskService(recallClient, clock, relevanceDecider);

		RecallRiskResponse response = service.analyze(new RiskDashboardAnalyzeRequest(
			"8471601020",
			"mechanical keyboard",
			"silent RGB wired keyboard",
			List.of(),
			"CN",
			new BigDecimal("100000"),
			"KRW",
			1,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			null,
			"keyboard",
			"keyboard",
			List.of("keyboard")
		));

		assertThat(relevanceDecider.normalized.primaryProductName()).isEqualTo("keyboard");
		assertThat(relevanceDecider.candidates).hasSize(2);
		assertThat(response.totalCount()).isEqualTo(1);
		assertThat(response.items()).extracting("productName").containsExactly("keyboard");
	}

	@Test
	void reusesPrefilteredRecallsWithoutCallingRecallSearchOrSecondAiFiltering() {
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient();
		FakeRecallRelevanceDecider relevanceDecider = new FakeRecallRelevanceDecider(List.of());
		RecallRiskService service = new RecallRiskService(recallClient, clock, relevanceDecider);

		RecallRiskResponse response = service.analyze(new RiskDashboardAnalyzeRequest(
			"8471601020",
			"mechanical keyboard",
			"silent RGB wired keyboard",
			List.of(),
			"CN",
			new BigDecimal("100000"),
			"KRW",
			1,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			null,
			"keyboard",
			"keyboard",
			List.of("keyboard"),
			List.of(new PrefilteredRecallRequest(
				"keyboard",
				"OPK50",
				"maker",
				"fire risk",
				"2025-11-20",
				0.97,
				"DOMESTIC"
			))
		));

		assertThat(recallClient.domesticProductQueries).isEmpty();
		assertThat(recallClient.foreignProductQueries).isEmpty();
		assertThat(relevanceDecider.candidates).isEmpty();
		assertThat(response.status()).isEqualTo(RiskStatus.WARNING);
		assertThat(response.totalCount()).isEqualTo(1);
		assertThat(response.items()).extracting("productName").containsExactly("keyboard");
		assertThat(response.items()).extracting("reason").containsExactly("fire risk");
		assertThat(response.items()).extracting("announcementDate").containsExactly("2025-11-20");
		assertThat(response.latestAnnouncementDate()).isEqualTo("2025-11-20");
	}

	@Test
	void returnsUnavailableWhenRecallApiFailsCompletely() {
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient();
		recallClient.failAll = true;
		RecallRiskService service = new RecallRiskService(recallClient, clock);

		RecallRiskResponse response = service.analyze(request());

		assertThat(response.status()).isEqualTo(RiskStatus.UNAVAILABLE);
	}

	private RiskDashboardAnalyzeRequest request() {
		return new RiskDashboardAnalyzeRequest(
			"3924100000",
			"baby plastic bowl",
			"plastic tableware for children",
			List.of("PVC", "phthalate"),
			"CN",
			new BigDecimal("100000"),
			"KRW",
			10,
			new BigDecimal("10000"),
			BigDecimal.ZERO,
			null
		);
	}

	private static class FakeSafetyKoreaRecallClient implements SafetyKoreaRecallClient {

		private final List<String> domesticProductQueries = new ArrayList<>();
		private final List<String> foreignProductQueries = new ArrayList<>();
		private List<SafetyKoreaRecallItem> domesticItems = List.of();
		private List<SafetyKoreaRecallItem> foreignItems = List.of();
		private boolean failAll;

		@Override
		public List<SafetyKoreaRecallItem> searchByProductName(String productName) {
			domesticProductQueries.add(productName);
			if (failAll) {
				throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED);
			}
			return domesticItems;
		}

		@Override
		public List<SafetyKoreaRecallItem> searchByBrandName(String brandName) {
			return List.of();
		}

		@Override
		public List<SafetyKoreaRecallItem> searchForeignByProductName(String productName) {
			foreignProductQueries.add(productName);
			if (failAll) {
				throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED);
			}
			return foreignItems;
		}

		@Override
		public List<SafetyKoreaRecallItem> searchForeignByBrandName(String brandName) {
			return List.of();
		}

		@Override
		public SafetyKoreaRecallItem findDetail(String recallUid) {
			return null;
		}
	}

	private static class FakeRecallRelevanceDecider implements RecallRelevanceDecider {

		private final List<SafetyKoreaRecallItem> selected;
		private ProductNormalizeResult normalized;
		private List<SafetyKoreaRecallItem> candidates = List.of();

		private FakeRecallRelevanceDecider(List<SafetyKoreaRecallItem> selected) {
			this.selected = selected;
		}

		@Override
		public List<SafetyKoreaRecallItem> selectRelevant(ProductNormalizeResult normalized, List<SafetyKoreaRecallItem> candidates) {
			this.normalized = normalized;
			this.candidates = candidates;
			return selected;
		}
	}
}
