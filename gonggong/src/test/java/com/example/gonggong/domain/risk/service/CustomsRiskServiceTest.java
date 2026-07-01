package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.risk.calculator.CustomsDutyCalculator;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.domain.TariffType;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.CustomsRiskResponse;
import com.example.gonggong.domain.risk.provider.TariffRateProvider;
import com.example.gonggong.domain.risk.provider.TariffRateResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CustomsRiskServiceTest {

	private static final String GUIDE_URL = "https://unipass.customs.go.kr/";
	private final Clock clock = Clock.fixed(
		Instant.parse("2026-06-25T00:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	@Test
	void calculatesRegisteredTariffRateAndDuty() {
		TariffRateProvider provider = (hskCode, originCountry, date) -> Optional.of(
			new TariffRateResult(
				TariffType.WTO,
				new BigDecimal("8.0"),
				BigDecimal.ZERO,
				"관세청_품목번호별 관세율표_20260211, 관세율구분=C"
			)
		);
		CustomsRiskService service = service(provider);

		CustomsRiskResponse response = service.analyze(request());

		assertThat(response.status()).isEqualTo(RiskStatus.SAFE);
		assertThat(response.finalTariffRate()).isEqualByComparingTo("8.0");
		assertThat(response.estimatedCustomsDuty()).isEqualByComparingTo("8800");
		assertThat(response.message()).contains("관세율구분=WTO 세율(C)");
	}

	@Test
	void warnsWhenSpecialAdditionalTariffApplies() {
		TariffRateProvider provider = (hskCode, originCountry, date) -> Optional.of(
			new TariffRateResult(
				TariffType.ANTI_DUMPING,
				new BigDecimal("8.0"),
				new BigDecimal("12.0"),
				"덤핑방지관세 고시"
			)
		);
		CustomsRiskService service = service(provider);

		CustomsRiskResponse response = service.analyze(request());

		assertThat(response.status()).isEqualTo(RiskStatus.WARNING);
		assertThat(response.finalTariffRate()).isEqualByComparingTo("20.0");
		assertThat(response.estimatedCustomsDuty()).isEqualByComparingTo("22000");
		assertThat(response.message()).contains("특별 가중관세", "덤핑방지관세 고시");
	}

	@Test
	void doesNotInventRateForUnregisteredHskCode() {
		TariffRateProvider provider = (hskCode, originCountry, date) -> Optional.empty();
		CustomsRiskService service = service(provider);

		CustomsRiskResponse response = service.analyze(request());

		assertThat(response.status()).isEqualTo(RiskStatus.UNKNOWN);
		assertThat(response.tariffType()).isEqualTo(TariffType.UNKNOWN);
		assertThat(response.finalTariffRate()).isNull();
		assertThat(response.estimatedCustomsDuty()).isNull();
		assertThat(response.guideUrl()).isEqualTo(GUIDE_URL);
	}

	@Test
	void doesNotCalculateDutyWhenDeclaredValueIsMissing() {
		TariffRateProvider provider = (hskCode, originCountry, date) -> Optional.of(
			new TariffRateResult(
				TariffType.WTO,
				new BigDecimal("5.0"),
				BigDecimal.ZERO,
				"관세청_품목번호별 관세율표_20260211, 관세율구분=C"
			)
		);
		CustomsRiskService service = service(provider);

		CustomsRiskResponse response = service.analyze(new RiskDashboardAnalyzeRequest(
			"3924100000",
			"유아용 플라스틱 식기",
			"어린이용 플라스틱 그릇",
			List.of(),
			"CN",
			BigDecimal.ZERO,
			"KRW",
			1,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			null
		));

		assertThat(response.finalTariffRate()).isEqualByComparingTo("5.0");
		assertThat(response.estimatedCustomsDuty()).isNull();
		assertThat(response.message()).contains("상품 가격 정보");
	}

	private CustomsRiskService service(TariffRateProvider provider) {
		return new CustomsRiskService(
			provider,
			new CustomsDutyCalculator(),
			GUIDE_URL,
			clock
		);
	}

	private RiskDashboardAnalyzeRequest request() {
		return new RiskDashboardAnalyzeRequest(
			"3924100000",
			"유아용 플라스틱 식기",
			"어린이용 플라스틱 그릇",
			List.of(),
			"CN",
			new BigDecimal("100000"),
			"KRW",
			10,
			new BigDecimal("10000"),
			BigDecimal.ZERO,
			null
		);
	}
}
