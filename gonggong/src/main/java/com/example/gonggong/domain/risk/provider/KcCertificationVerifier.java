package com.example.gonggong.domain.risk.provider;

import java.util.List;

@FunctionalInterface
public interface KcCertificationVerifier {

	KcCertificationVerificationResult verify(
		String certificationNumber,
		String productName,
		String modelName,
		String brandName,
		List<String> kcCertificationSearchKeywords
	);
}
