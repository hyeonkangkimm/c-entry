package com.example.gonggong.domain.risk.provider;

public record KcCertificationCandidateDecision(
	String candidateId,
	KcCertificationMatchType matchType,
	String reason
) {
	public static KcCertificationCandidateDecision none(String reason) {
		return new KcCertificationCandidateDecision(null, KcCertificationMatchType.NONE, reason);
	}
}
