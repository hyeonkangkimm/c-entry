package com.example.gonggong.domain.risk.provider;

public record KcRequirementResult(
	boolean certificationRequired,
	String certificationType,
	String relatedLaw,
	String approvalAgency,
	String source
) {
	public KcRequirementResult(
		boolean certificationRequired,
		String certificationType,
		String relatedLaw
	) {
		this(certificationRequired, certificationType, relatedLaw, null, null);
	}
}
