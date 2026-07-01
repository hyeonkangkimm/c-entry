package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.risk.calculator.CustomsDutyCalculator;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.domain.TariffType;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.CustomsRiskResponse;
import com.example.gonggong.domain.risk.provider.TariffRateProvider;
import com.example.gonggong.domain.risk.provider.TariffRateResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Service
public class CustomsRiskService {

	private static final Set<TariffType> SPECIAL_TARIFF_TYPES = EnumSet.of(
		TariffType.ANTI_DUMPING,
		TariffType.COUNTERVAILING,
		TariffType.SAFEGUARD,
		TariffType.SPECIAL
	);

	private final TariffRateProvider tariffRateProvider;
	private final CustomsDutyCalculator dutyCalculator;
	private final String unipassUrl;
	private final Clock clock;

	@Autowired
	public CustomsRiskService(
		TariffRateProvider tariffRateProvider,
		CustomsDutyCalculator dutyCalculator,
		@Value("${public-data.customs.unipass-url:https://unipass.customs.go.kr/}") String unipassUrl
	) {
		this(tariffRateProvider, dutyCalculator, unipassUrl, Clock.systemDefaultZone());
	}

	CustomsRiskService(
		TariffRateProvider tariffRateProvider,
		CustomsDutyCalculator dutyCalculator,
		String unipassUrl,
		Clock clock
	) {
		this.tariffRateProvider = tariffRateProvider;
		this.dutyCalculator = dutyCalculator;
		this.unipassUrl = unipassUrl;
		this.clock = clock;
	}

	public CustomsRiskResponse analyze(RiskDashboardAnalyzeRequest request) {
		TariffRateResult rate = tariffRateProvider.findTariffRate(
			request.hskCode(),
			request.originCountry(),
			LocalDate.now(clock)
		).orElse(null);

		if (rate == null) {
			return unknown("미등록 코드입니다. 관세청 정식 법령 지침을 확인해 주세요.");
		}

		BigDecimal finalRate = rate.finalRate();
		boolean supportedCurrency = request.currency() == null
			|| request.currency().isBlank()
			|| "KRW".equalsIgnoreCase(request.currency());
		boolean hasDeclaredValue = request.declaredValue() != null
			&& request.declaredValue().compareTo(BigDecimal.ZERO) > 0;
		BigDecimal estimatedDuty = null;
		if (supportedCurrency && hasDeclaredValue) {
			BigDecimal taxableValue = dutyCalculator.taxableValue(
				request.safeDeclaredValue(),
				request.safeShippingCost(),
				request.safeInsuranceCost()
			);
			estimatedDuty = dutyCalculator.customsDuty(taxableValue, finalRate);
		}

		boolean specialTariff = SPECIAL_TARIFF_TYPES.contains(rate.tariffType())
			|| rate.additionalRate().compareTo(BigDecimal.ZERO) > 0;
		RiskStatus status = specialTariff ? RiskStatus.WARNING : RiskStatus.SAFE;
		int score = specialTariff ? 70 : 20;
		String message = message(rate, supportedCurrency, hasDeclaredValue, specialTariff);

		return new CustomsRiskResponse(
			status,
			score,
			rate.tariffType(),
			rate.baseRate(),
			rate.additionalRate(),
			finalRate,
			estimatedDuty,
			message,
			unipassUrl
		);
	}

	private CustomsRiskResponse unknown(String message) {
		return new CustomsRiskResponse(
			RiskStatus.UNKNOWN,
			50,
			TariffType.UNKNOWN,
			null,
			null,
			null,
			null,
			message,
			unipassUrl
		);
	}

	private String message(
		TariffRateResult rate,
		boolean supportedCurrency,
		boolean hasDeclaredValue,
		boolean specialTariff
	) {
		String baseMessage = specialTariff
			? "본 품목은 특별 가중관세 부과 대상 등록 품목입니다."
			: "등록된 관세율 기준으로 예상 관세액을 계산했습니다.";
		if (!supportedCurrency) {
			baseMessage += " 환율 데이터가 없어 예상 관세액은 계산하지 않았습니다.";
		}
		else if (!hasDeclaredValue) {
			baseMessage += " 상품 가격 정보가 없어 예상 관세액은 계산하지 않았습니다.";
		}
		if (rate.legalNotice() != null && !rate.legalNotice().isBlank()) {
			baseMessage += " 근거: " + readableLegalNotice(rate);
		}
		return baseMessage + " 실제 적용 세율은 원산지 증명과 세관 판단에 따라 달라질 수 있습니다.";
	}

	private String readableLegalNotice(TariffRateResult rate) {
		String label = tariffTypeLabel(rate.tariffType());
		return rate.legalNotice()
			.replace("관세율구분=A", "관세율구분=" + label + "(A)")
			.replace("관세율구분=C", "관세율구분=" + label + "(C)");
	}

	private String tariffTypeLabel(TariffType tariffType) {
		return switch (tariffType) {
			case BASIC -> "기본세율";
			case WTO -> "WTO 세율";
			case FTA -> "FTA 협정세율";
			case ANTI_DUMPING -> "덤핑방지관세";
			case COUNTERVAILING -> "상계관세";
			case SAFEGUARD -> "긴급관세";
			case SPECIAL -> "특별관세";
			case UNKNOWN -> "관세율 확인 필요";
		};
	}
}
