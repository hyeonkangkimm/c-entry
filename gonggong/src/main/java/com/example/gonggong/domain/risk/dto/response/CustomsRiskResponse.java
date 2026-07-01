package com.example.gonggong.domain.risk.dto.response;

import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.domain.TariffType;

import java.math.BigDecimal;

public record CustomsRiskResponse(
	RiskStatus status,
	int score,
	TariffType tariffType,
	BigDecimal baseTariffRate,
	BigDecimal additionalTariffRate,
	BigDecimal finalTariffRate,
	BigDecimal estimatedCustomsDuty,
	String message,
	String guideUrl
) {
}
