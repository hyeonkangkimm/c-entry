package com.example.gonggong.domain.risk.provider;

import com.example.gonggong.domain.risk.domain.TariffType;

import java.math.BigDecimal;

public record TariffRateResult(
	TariffType tariffType,
	BigDecimal baseRate,
	BigDecimal additionalRate,
	String legalNotice
) {
	public BigDecimal finalRate() {
		return baseRate.add(additionalRate);
	}
}
