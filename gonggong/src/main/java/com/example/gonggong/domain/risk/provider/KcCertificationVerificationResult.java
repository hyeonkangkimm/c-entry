package com.example.gonggong.domain.risk.provider;

public record KcCertificationVerificationResult(
	KcCertificationVerificationStatus status,
	boolean valid,
	String certificationType,
	String relatedLaw,
	String message
) {
	public static KcCertificationVerificationResult valid(
		String certificationType,
		String relatedLaw,
		String message
	) {
		return new KcCertificationVerificationResult(
			KcCertificationVerificationStatus.VALID,
			true,
			certificationType,
			relatedLaw,
			message == null || message.isBlank() ? "인증 확인 완료" : message
		);
	}

	public static KcCertificationVerificationResult unknown(String message) {
		return new KcCertificationVerificationResult(
			KcCertificationVerificationStatus.UNKNOWN,
			false,
			null,
			null,
			message
		);
	}

	public static KcCertificationVerificationResult unavailable(String message) {
		return new KcCertificationVerificationResult(
			KcCertificationVerificationStatus.UNAVAILABLE,
			false,
			null,
			null,
			message
		);
	}
}
