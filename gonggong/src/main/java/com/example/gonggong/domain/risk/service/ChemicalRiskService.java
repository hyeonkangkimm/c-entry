package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.risk.chemical.ChemicalApiProperties;
import com.example.gonggong.domain.risk.chemical.ChemicalClassification;
import com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate;
import com.example.gonggong.domain.risk.chemical.ChemicalInformationClient;
import com.example.gonggong.domain.risk.chemical.ChemicalLookupResult;
import com.example.gonggong.domain.risk.chemical.ChemicalLookupStatus;
import com.example.gonggong.domain.risk.chemical.ChemicalRegulationRule;
import com.example.gonggong.domain.risk.chemical.ChemicalRegulationRuleRepository;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.ChemicalRiskResponse;
import com.example.gonggong.domain.risk.dto.response.RegulatedIngredientResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Service
public class ChemicalRiskService {

	private static final Logger log = LoggerFactory.getLogger(ChemicalRiskService.class);

	private final ChemicalInformationClient client;
	private final ChemicalRegulationRuleRepository rules;
	private final ChemicalApiProperties properties;
	private final Clock clock;
	private final ExecutorService executor;

	@Autowired
	public ChemicalRiskService(
		ChemicalInformationClient client,
		ChemicalRegulationRuleRepository rules,
		ChemicalApiProperties properties
	) {
		this(client, rules, properties, Clock.systemUTC(),
			Executors.newFixedThreadPool(Math.max(1, properties.getConcurrency())));
	}

	ChemicalRiskService(
		ChemicalInformationClient client,
		ChemicalRegulationRuleRepository rules,
		ChemicalApiProperties properties,
		Clock clock,
		ExecutorService executor
	) {
		this.client = client;
		this.rules = rules;
		this.properties = properties;
		this.clock = clock;
		this.executor = executor;
	}

	public ChemicalRiskResponse analyze(RiskDashboardAnalyzeRequest request) {
		List<ChemicalIngredientCandidate> candidates = request.normalizedChemicalCandidates();
		log.info("Chemical risk analyze candidateCount={} candidates={}", candidates.size(), candidates);
		if (candidates.isEmpty()) {
			return response(RiskStatus.UNKNOWN, 30, "시스템 성분 분석 불가능 품목", List.of(), List.of(), true);
		}

		List<CompletableFuture<ChemicalLookupResult>> futures = candidates.stream()
			.map(candidate -> CompletableFuture.supplyAsync(() -> safeLookup(candidate), executor))
			.toList();
		List<ChemicalLookupResult> results = futures.stream().map(CompletableFuture::join).toList();
		List<String> unanalyzed = results.stream()
			.filter(result -> result.status() != ChemicalLookupStatus.MATCHED)
			.map(ChemicalLookupResult::requestedIngredient)
			.toList();
		List<RegulatedIngredientResponse> ingredients = ingredientDetails(results);
		long regulatedCount = ingredients.stream().filter(RegulatedIngredientResponse::regulated).count();

		if (regulatedCount > 0) {
			return response(RiskStatus.DANGER, 90, "국내법상 규제 가능성이 있는 성분이 확인되었습니다.",
				ingredients, unanalyzed, !unanalyzed.isEmpty());
		}
		long unavailableCount = results.stream().filter(r -> r.status() == ChemicalLookupStatus.UNAVAILABLE).count();
		long matchedCount = results.stream().filter(r -> r.status() == ChemicalLookupStatus.MATCHED).count();
		if (unavailableCount == results.size()) {
			return response(RiskStatus.UNAVAILABLE, 50, "시스템 성분 분석 불가능 품목", ingredients, unanalyzed, true);
		}
		if (!unanalyzed.isEmpty()) {
			RiskStatus status = matchedCount == 0 && unavailableCount == 0 ? RiskStatus.UNKNOWN : RiskStatus.WARNING;
			return response(status, 55, "시스템 성분 분석 불가능 품목", ingredients, unanalyzed, true);
		}
		return response(RiskStatus.SAFE, 15, "조회된 성분에서 등록된 국내 수입 규제 신호를 확인하지 못했습니다.",
			ingredients, List.of(), false);
	}

	private ChemicalLookupResult safeLookup(ChemicalIngredientCandidate candidate) {
		ChemicalLookupResult lastUnavailable = null;
		log.info(
			"Chemical lookup candidate name={} casNumber={} englishName={}",
			candidate.name(),
			candidate.casNumber(),
			candidate.englishName()
		);
		for (String query : lookupQueries(candidate)) {
			try {
				log.info(
					"Chemical lookup query candidateName={} query={} order={}",
					candidate.name(),
					query,
					lookupOrder(candidate, query)
				);
				ChemicalLookupResult result = client.lookup(query);
				log.info(
					"Chemical lookup result candidateName={} query={} status={} requestedIngredient={} reason={}",
					candidate.name(),
					query,
					result.status(),
					result.requestedIngredient(),
					result.reason()
				);
				if (result.status() == ChemicalLookupStatus.MATCHED) {
					return result;
				}
				if (result.status() == ChemicalLookupStatus.UNAVAILABLE) {
					lastUnavailable = result;
				}
			}
			catch (RuntimeException exception) {
				lastUnavailable = ChemicalLookupResult.unavailable(query, "lookup-failed");
			}
		}
		if (lastUnavailable != null) {
			return lastUnavailable;
		}
		return ChemicalLookupResult.notFound(firstNonBlank(candidate.name(), candidate.englishName(), candidate.casNumber()));
	}

	private List<String> lookupQueries(ChemicalIngredientCandidate candidate) {
		return Stream.of(candidate.casNumber(), candidate.englishName(), candidate.name())
			.filter(value -> value != null && !value.isBlank())
			.distinct()
			.toList();
	}

	private String lookupOrder(ChemicalIngredientCandidate candidate, String query) {
		if (query != null && query.equals(candidate.casNumber())) {
			return "CAS";
		}
		if (query != null && query.equals(candidate.englishName())) {
			return "ENGLISH";
		}
		if (query != null && query.equals(candidate.name())) {
			return "KOREAN";
		}
		return "UNKNOWN";
	}

	private List<RegulatedIngredientResponse> ingredientDetails(List<ChemicalLookupResult> results) {
		List<RegulatedIngredientResponse> ingredients = new ArrayList<>();
		LocalDate today = LocalDate.now(clock);
		for (ChemicalLookupResult result : results) {
			if (result.status() != ChemicalLookupStatus.MATCHED || result.substance() == null) continue;
			for (ChemicalClassification classification : result.substance().classifications()) {
				Optional<ChemicalRegulationRule> matchedRule = rules.findActive(classification.identifier(), today)
					.filter(ChemicalRegulationRule::regulated)
					.or(() -> inferredRule(classification));
				ChemicalRegulationRule rule = matchedRule.orElse(null);
				boolean regulatedIngredient = rule != null;
				ingredients.add(new RegulatedIngredientResponse(
					firstNonBlank(result.substance().koreanName(), result.requestedIngredient()),
					result.substance().casNumber(),
					firstNonBlank(classification.type(), rule == null ? null : rule.classificationName(), "분류 정보 없음"),
					rule == null ? classification.type() : rule.relatedLaw(),
					rule == null ? null : rule.verifiedPenalty(),
					rule == null ? null : rule.obligationSourceUrl(),
					rule == null || rule.verifiedPenalty() == null ? null : rule.penaltySourceUrl(),
					regulatedIngredient
				));
			}
			if (result.substance().classifications().isEmpty()) {
				ingredients.add(new RegulatedIngredientResponse(
					firstNonBlank(result.substance().koreanName(), result.requestedIngredient()),
					result.substance().casNumber(),
					"분류 정보 없음",
					null,
					null,
					null,
					null,
					false
				));
			}
		}
		return List.copyOf(ingredients);
	}

	private Optional<ChemicalRegulationRule> inferredRule(ChemicalClassification classification) {
		String type = classification.type() == null ? "" : classification.type().replaceAll("\\s+", "");
		boolean explicitlyRegulated = List.of("유독물질", "제한물질", "금지물질", "허가물질", "사고대비물질")
			.stream()
			.anyMatch(type::contains);
		if (!explicitlyRegulated) return Optional.empty();
		return Optional.of(new ChemicalRegulationRule(
			classification.identifier(), classification.type(), true,
			"화학물질관리법", null,
			"https://www.law.go.kr/법령/화학물질관리법",
			null, null, null, null, null
		));
	}

	private ChemicalRiskResponse response(
		RiskStatus status,
		int score,
		String message,
		List<RegulatedIngredientResponse> regulated,
		List<String> unanalyzed,
		boolean unavailable
	) {
		return new ChemicalRiskResponse(status, score, message, regulated, properties.getSearchUrl(),
			unanalyzed, unavailable, properties.getSearchButtonText());
	}

	private String firstNonBlank(String... values) {
		for (String value : values) if (value != null && !value.isBlank()) return value.trim();
		return null;
	}

	@PreDestroy
	void shutdownExecutor() {
		executor.shutdownNow();
	}
}

