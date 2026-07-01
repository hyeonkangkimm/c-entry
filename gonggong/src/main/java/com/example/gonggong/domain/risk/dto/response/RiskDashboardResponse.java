package com.example.gonggong.domain.risk.dto.response;

import com.example.gonggong.domain.risk.domain.OverallRiskLevel;

import java.time.LocalDateTime;
import java.util.List;

public record RiskDashboardResponse(
	Long analysisId,
	String hskCode,
	String productName,
	OverallRiskLevel overallRiskLevel,
	int overallRiskScore,
	LocalDateTime analyzedAt,
	RecallRiskResponse recallRisk,
	CustomsRiskResponse customsRisk,
	KcRiskResponse kcRisk,
	ChemicalRiskResponse chemicalRisk,
	List<String> warnings
) {
}
