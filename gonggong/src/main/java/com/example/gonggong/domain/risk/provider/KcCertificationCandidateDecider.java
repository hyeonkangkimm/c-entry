package com.example.gonggong.domain.risk.provider;

import java.util.Map;

public interface KcCertificationCandidateDecider {

	KcCertificationCandidateDecision decide(
		String productName,
		String modelName,
		String brandName,
		Map<String, SafetyKoreaCertificationItem> candidateById
	);
}
