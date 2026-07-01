package com.example.gonggong.domain.risk.dto.response;

public record RecallRiskItemResponse(
	String productName,
	String reason,
	String violationDetails,
	String announcementDate,
	String sourceUrl,
	String source
) {
}
