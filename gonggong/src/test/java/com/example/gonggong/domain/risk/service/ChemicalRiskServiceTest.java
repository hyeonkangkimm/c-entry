package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.risk.chemical.ChemicalApiProperties;
import com.example.gonggong.domain.risk.chemical.ChemicalClassification;
import com.example.gonggong.domain.risk.chemical.ChemicalInformationClient;
import com.example.gonggong.domain.risk.chemical.ChemicalLookupResult;
import com.example.gonggong.domain.risk.chemical.ChemicalRegulationRule;
import com.example.gonggong.domain.risk.chemical.ChemicalRegulationRuleRepository;
import com.example.gonggong.domain.risk.chemical.ChemicalSubstance;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.ChemicalRiskResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class ChemicalRiskServiceTest {

	private final ExecutorService executor = Executors.newFixedThreadPool(4);

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void returnsDangerWithVerifiedRegulationAndNoUnverifiedPenalty() {
		ChemicalInformationClient client = ingredient -> ChemicalLookupResult.matched(
			ingredient,
			new ChemicalSubstance("폼알데하이드", "Formaldehyde", "50-00-0", List.of(
				new ChemicalClassification("유독물질", "97-1-5", "0.1% 이상", null, "2025-08-07", "지정")
			))
		);
		ChemicalRegulationRule rule = new ChemicalRegulationRule(
			"97-1-5", "유독물질", true, "화학물질관리법", "제20조",
			"https://www.law.go.kr/법령/화학물질관리법", null, null, null,
			LocalDate.of(2025, 8, 7), null
		);
		ChemicalRiskService service = service(client, (identifier, date) -> Optional.of(rule));

		ChemicalRiskResponse response = service.analyze(request(List.of("formaldehyde")));

		assertThat(response.status()).isEqualTo(RiskStatus.DANGER);
		assertThat(response.analysisUnavailable()).isFalse();
		assertThat(response.regulatedIngredients()).singleElement().satisfies(item -> {
			assertThat(item.ingredientName()).isEqualTo("폼알데하이드");
			assertThat(item.casNumber()).isEqualTo("50-00-0");
			assertThat(item.hazardClassification()).isEqualTo("유독물질");
			assertThat(item.relatedLaw()).isEqualTo("화학물질관리법 제20조");
			assertThat(item.penaltyProvision()).isNull();
			assertThat(item.legalSourceUrl()).startsWith("https://www.law.go.kr/");
		});
	}

	@Test
	void searchesStructuredChemicalCandidatesByCasBeforeNames() {
		List<String> lookups = new java.util.concurrent.CopyOnWriteArrayList<>();
		ChemicalInformationClient client = ingredient -> {
			lookups.add(ingredient);
			if ("50-00-0".equals(ingredient)) {
				return ChemicalLookupResult.matched(
					ingredient,
					new ChemicalSubstance("폼알데하이드", "Formaldehyde", "50-00-0", List.of())
				);
			}
			return ChemicalLookupResult.notFound(ingredient);
		};
		ChemicalRiskService service = service(client, (identifier, date) -> Optional.empty());

		ChemicalRiskResponse response = service.analyze(requestWithCandidates(List.of(
			new com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate("납", "50-00-0", "lead")
		)));

		assertThat(lookups).containsExactly("50-00-0");
		assertThat(response.status()).isEqualTo(RiskStatus.SAFE);
	}

	@Test
	void returnsWarningWhenOnlySomeIngredientsCannotBeAnalyzed() {
		ChemicalInformationClient client = ingredient -> ingredient.equals("known")
			? ChemicalLookupResult.notFound(ingredient)
			: ChemicalLookupResult.unavailable(ingredient, "timeout");
		ChemicalRiskService service = service(client, (identifier, date) -> Optional.empty());

		ChemicalRiskResponse response = service.analyze(request(List.of("known", "unknown")));

		assertThat(response.status()).isEqualTo(RiskStatus.WARNING);
		assertThat(response.analysisUnavailable()).isTrue();
		assertThat(response.unanalyzedIngredients()).containsExactly("known", "unknown");
	}

	@Test
	void treatsExplicitApiRegulationTypeAsDangerWithoutInventingPenalty() {
		ChemicalInformationClient client = ingredient -> ChemicalLookupResult.matched(
			ingredient,
			new ChemicalSubstance("벤젠", "Benzene", "71-43-2", List.of(
				new ChemicalClassification("제한물질", "unconfirmed-id", null, null, null, null)
			))
		);
		ChemicalRiskService service = service(client, (identifier, date) -> Optional.empty());

		ChemicalRiskResponse response = service.analyze(request(List.of("benzene")));

		assertThat(response.status()).isEqualTo(RiskStatus.DANGER);
		assertThat(response.regulatedIngredients()).singleElement().satisfies(item -> {
			assertThat(item.relatedLaw()).isEqualTo("화학물질관리법");
			assertThat(item.penaltyProvision()).isNull();
		});
	}

	@Test
	void returnsUnavailableWhenEveryLookupIsUnavailable() {
		ChemicalRiskService service = service(
			ingredient -> ChemicalLookupResult.unavailable(ingredient, "api-not-configured"),
			(identifier, date) -> Optional.empty()
		);

		ChemicalRiskResponse response = service.analyze(request(List.of("one", "two")));

		assertThat(response.status()).isEqualTo(RiskStatus.UNAVAILABLE);
		assertThat(response.unanalyzedIngredients()).containsExactly("one", "two");
		assertThat(response.searchButtonText()).contains("직접 성분 검색하기");
	}

	private ChemicalRiskService service(ChemicalInformationClient client, ChemicalRegulationRuleRepository rules) {
		ChemicalApiProperties properties = new ChemicalApiProperties();
		return new ChemicalRiskService(
			client,
			rules,
			properties,
			Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC),
			executor
		);
	}

	private RiskDashboardAnalyzeRequest request(List<String> ingredients) {
		return new RiskDashboardAnalyzeRequest(
			"2912110000", "화학제품", "성분 포함", ingredients, "CN",
			BigDecimal.TEN, "USD", 1, BigDecimal.ZERO, BigDecimal.ZERO, null
		);
	}

	private RiskDashboardAnalyzeRequest requestWithCandidates(List<com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate> candidates) {
		return new RiskDashboardAnalyzeRequest(
			"2912110000", "화학제품", "성분 포함", List.of(), candidates, "CN",
			BigDecimal.TEN, "USD", 1, BigDecimal.ZERO, BigDecimal.ZERO, null
		);
	}
}
