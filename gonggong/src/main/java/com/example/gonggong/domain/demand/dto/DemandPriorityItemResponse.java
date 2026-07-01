package com.example.gonggong.domain.demand.dto;

import java.math.BigDecimal;

public record DemandPriorityItemResponse(
	String hskCode,
	String itemName,
	BigDecimal importAmountGrowthRate,
	BigDecimal importWeightGrowthRate,
	int recallCount,
	boolean essentialPart,
	int priorityScore,
	String priorityLevel,
	String reason
) {
}
