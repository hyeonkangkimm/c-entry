package com.example.gonggong.domain.analysis.dto;

public record MatchedRecallDto(
	String recallProductName,
	String modelName,
	String manufacturer,
	String reason,
	String announcementDate,
	double similarity,
	String source
) {
	public MatchedRecallDto(
		String recallProductName,
		String modelName,
		String manufacturer,
		String reason,
		double similarity
	) {
		this(recallProductName, modelName, manufacturer, reason, null, similarity, "DOMESTIC");
	}
}
