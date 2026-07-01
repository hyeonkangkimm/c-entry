package com.example.gonggong.domain.risk.dto.response;

import com.example.gonggong.domain.risk.domain.RiskStatus;

import java.util.List;

public record RecallRiskResponse(
	RiskStatus status,
	int score,
	int totalCount,
	String latestAnnouncementDate,
	String message,
	List<RecallRiskItemResponse> items
) {
}
