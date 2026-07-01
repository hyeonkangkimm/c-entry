package com.example.gonggong.domain.risk.dto.request;

public record PrefilteredRecallRequest(
	String recallProductName,
	String modelName,
	String manufacturer,
	String reason,
	String announcementDate,
	Double similarity,
	String source
) {
}
