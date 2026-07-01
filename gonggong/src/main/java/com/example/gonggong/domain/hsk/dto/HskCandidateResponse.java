package com.example.gonggong.domain.hsk.dto;

public record HskCandidateResponse(
	String hskCode,
	String koreanName,
	String englishName,
	String displayName,
	double confidence,
	String reason
) {
	public HskCandidateResponse(String hskCode, String koreanName, String englishName, double confidence, String reason) {
		this(hskCode, koreanName, englishName, koreanName, confidence, reason);
	}
}
