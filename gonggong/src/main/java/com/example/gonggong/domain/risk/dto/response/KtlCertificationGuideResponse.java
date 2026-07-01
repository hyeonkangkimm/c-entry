package com.example.gonggong.domain.risk.dto.response;

import java.time.LocalDate;
import java.util.List;

public record KtlCertificationGuideResponse(
	String certificationName,
	String certificationMarkUrl,
	String legalBasis,
	List<String> testItems,
	List<String> requiredDocuments,
	String estimatedDuration,
	String estimatedFee,
	String applicationUrl,
	String actionItemGuide,
	String sourceUrl,
	LocalDate verifiedAt
) {
}
