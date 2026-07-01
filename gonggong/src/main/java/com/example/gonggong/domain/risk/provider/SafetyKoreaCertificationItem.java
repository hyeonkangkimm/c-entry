package com.example.gonggong.domain.risk.provider;

public record SafetyKoreaCertificationItem(
	String certificationNumber,
	String productName,
	String modelName,
	String brandName,
	String statusName,
	String certificationType,
	String relatedLaw
) {
}
