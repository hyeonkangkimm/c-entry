package com.example.gonggong.domain.risk.dto.response;

import com.example.gonggong.domain.risk.domain.RiskStatus;

public record KcRiskResponse(
	RiskStatus status,
	int score,
	Boolean certificationRequired,
	boolean certificationValid,
	String certificationNumber,
	String certificationType,
	String relatedLaw,
	String message,
	String verificationUrl,
	String verificationButtonText,
	KtlCertificationGuideResponse ktlGuide
) {
	public KcRiskResponse(
		RiskStatus status,
		int score,
		Boolean certificationRequired,
		boolean certificationValid,
		String certificationNumber,
		String certificationType,
		String relatedLaw,
		String message,
		String verificationUrl,
		String verificationButtonText
	) {
		this(status, score, certificationRequired, certificationValid, certificationNumber, certificationType,
			relatedLaw, message, verificationUrl, verificationButtonText, null);
	}
}
