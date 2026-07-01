package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.risk.calculator.RiskScoreCalculator;
import com.example.gonggong.domain.risk.chemical.OpenAiChemicalCandidateTranslator;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.ChemicalRiskResponse;
import com.example.gonggong.domain.risk.dto.response.CustomsRiskResponse;
import com.example.gonggong.domain.risk.dto.response.KcRiskResponse;
import com.example.gonggong.domain.risk.dto.response.RecallRiskResponse;
import com.example.gonggong.domain.risk.dto.response.RiskDashboardResponse;
import com.example.gonggong.global.logging.DomPayloadLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskDashboardService {

	private static final Logger log = LoggerFactory.getLogger(RiskDashboardService.class);

	private final RecallRiskService recallRiskService;
	private final CustomsRiskService customsRiskService;
	private final KcRiskService kcRiskService;
	private final ChemicalRiskService chemicalRiskService;
	private final OpenAiChemicalCandidateTranslator chemicalCandidateTranslator;
	private final RiskScoreCalculator riskScoreCalculator;

	@org.springframework.beans.factory.annotation.Autowired
	public RiskDashboardService(
		RecallRiskService recallRiskService,
		CustomsRiskService customsRiskService,
		KcRiskService kcRiskService,
		ChemicalRiskService chemicalRiskService,
		OpenAiChemicalCandidateTranslator chemicalCandidateTranslator,
		RiskScoreCalculator riskScoreCalculator
	) {
		this.recallRiskService = recallRiskService;
		this.customsRiskService = customsRiskService;
		this.kcRiskService = kcRiskService;
		this.chemicalRiskService = chemicalRiskService;
		this.chemicalCandidateTranslator = chemicalCandidateTranslator;
		this.riskScoreCalculator = riskScoreCalculator;
	}

	public RiskDashboardService(
		RecallRiskService recallRiskService,
		CustomsRiskService customsRiskService,
		KcRiskService kcRiskService,
		ChemicalRiskService chemicalRiskService,
		RiskScoreCalculator riskScoreCalculator
	) {
		this(recallRiskService, customsRiskService, kcRiskService, chemicalRiskService, null, riskScoreCalculator);
	}

	public RiskDashboardResponse analyze(RiskDashboardAnalyzeRequest request) {
		logDomRiskDashboardRequest(request);
		RiskDashboardAnalyzeRequest translatedRequest = translateChemicalCandidates(request);
		RecallRiskResponse recallRisk = recallRiskService.analyze(translatedRequest);
		CustomsRiskResponse customsRisk = customsRiskService.analyze(translatedRequest);
		KcRiskResponse kcRisk = kcRiskService.analyze(translatedRequest);
		ChemicalRiskResponse chemicalRisk = chemicalRiskService.analyze(translatedRequest);

		int overallScore = riskScoreCalculator.calculateOverallScore(
			recallRisk.score(),
			customsRisk.score(),
			kcRisk.score(),
			chemicalRisk.score()
		);
		List<String> warnings = warnings(customsRisk, kcRisk, chemicalRisk);

		return new RiskDashboardResponse(
			null,
			request.hskCode(),
			request.productName(),
			riskScoreCalculator.toOverallRiskLevel(
				overallScore,
				0,
				recallRisk.status(),
				customsRisk.status(),
				kcRisk.status(),
				chemicalRisk.status()
			),
			overallScore,
			LocalDateTime.now(),
			recallRisk,
			customsRisk,
			kcRisk,
			chemicalRisk,
			warnings
		);
	}

	private RiskDashboardAnalyzeRequest translateChemicalCandidates(RiskDashboardAnalyzeRequest request) {
		if (chemicalCandidateTranslator == null) {
			return request;
		}
		List<String> ingredients = request.ingredients() == null ? List.of() : request.ingredients().stream()
			.filter(value -> value != null && !value.isBlank())
			.map(String::trim)
			.distinct()
			.toList();
		if (ingredients.isEmpty()) {
			return request;
		}
		List<com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate> translatedCandidates =
			chemicalCandidateTranslator.translate(ingredients);
		if (translatedCandidates.isEmpty()) {
			return request;
		}
		log.info("Chemical candidates translated ingredientCount={} candidateCount={}", ingredients.size(), translatedCandidates.size());
		return request.withChemicalCandidates(translatedCandidates);
	}

	private void logDomRiskDashboardRequest(RiskDashboardAnalyzeRequest request) {
		log.info(
			"DOM product payload received endpoint=risk-dashboard hskCode={} productName={} description={} standardProductName={} primaryProductName={} modelName={} brandName={} kcCertificationNumber={} kcCertificationType={} primarySearchKeywords={} kcCertificationSearchKeywords={} ingredients={} declaredValue={} currency={} originCountry={} prefilteredRecallCount={}",
			request.hskCode(),
			DomPayloadLogFormatter.clip(request.productName(), 120),
			DomPayloadLogFormatter.clip(request.productDescription(), 220),
			DomPayloadLogFormatter.clip(request.standardProductName(), 100),
			DomPayloadLogFormatter.clip(request.primaryProductName(), 100),
			DomPayloadLogFormatter.clip(request.modelName(), 80),
			DomPayloadLogFormatter.clip(request.brandName(), 80),
			DomPayloadLogFormatter.maskCertificationNumber(request.kcCertificationNumber()),
			DomPayloadLogFormatter.clip(request.normalizedKcCertificationType(), 80),
			DomPayloadLogFormatter.clipList(request.primarySearchKeywords(), 8, 40),
			DomPayloadLogFormatter.clipList(request.normalizedKcCertificationSearchKeywords(), 8, 40),
			DomPayloadLogFormatter.clipList(request.normalizedIngredients(), 8, 40),
			request.declaredValue(),
			request.currency(),
			request.originCountry(),
			request.normalizedPrefilteredRecalls().size()
		);
	}

	private List<String> warnings(
		CustomsRiskResponse customsRisk,
		KcRiskResponse kcRisk,
		ChemicalRiskResponse chemicalRisk
	) {
		List<String> warnings = new ArrayList<>();
		if (customsRisk.status() == com.example.gonggong.domain.risk.domain.RiskStatus.UNKNOWN
			|| customsRisk.status() == com.example.gonggong.domain.risk.domain.RiskStatus.UNAVAILABLE) {
			warnings.add("등록된 관세율 데이터가 없어 관세청 정식 법령 지침 확인이 필요합니다.");
		}
		if (kcRisk.status() == com.example.gonggong.domain.risk.domain.RiskStatus.UNKNOWN
			|| kcRisk.status() == com.example.gonggong.domain.risk.domain.RiskStatus.UNAVAILABLE) {
			warnings.add("KC 인증번호 유효성은 제품안전정보센터에서 실시간 검증이 필요합니다.");
		}
		if (chemicalRisk.analysisUnavailable()) {
			warnings.add("일부 또는 전체 성분을 자동 분석하지 못해 화학물질 종합정보시스템 직접 확인이 필요합니다.");
		}
		return warnings;
	}
}
