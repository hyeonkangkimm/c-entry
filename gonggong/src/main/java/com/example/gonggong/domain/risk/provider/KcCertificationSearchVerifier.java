package com.example.gonggong.domain.risk.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class KcCertificationSearchVerifier implements KcCertificationVerifier {

	private static final Logger log = LoggerFactory.getLogger(KcCertificationSearchVerifier.class);

	private final SafetyKoreaCertificationClient certificationClient;

	@Autowired
	public KcCertificationSearchVerifier(SafetyKoreaCertificationClient certificationClient) {
		this.certificationClient = certificationClient;
	}

	public KcCertificationSearchVerifier(
		SafetyKoreaCertificationClient certificationClient,
		@SuppressWarnings("unused") KcCertificationCandidateDecider candidateDecider
	) {
		this(certificationClient);
	}

	public KcCertificationVerificationResult verify(
		String certificationNumber,
		String productName,
		String modelName,
		String brandName
	) {
		return verify(certificationNumber, productName, modelName, brandName, List.of());
	}

	@Override
	public KcCertificationVerificationResult verify(
		String certificationNumber,
		String productName,
		String modelName,
		String brandName,
		List<String> kcCertificationSearchKeywords
	) {
		if (!hasText(certificationNumber)) {
			return KcCertificationVerificationResult.unknown("KC 인증번호가 없어 제품안전정보센터 인증 DB를 조회하지 않았습니다.");
		}

		List<SafetyKoreaCertificationItem> candidates = certificationClient.searchByCertificationNumber(certificationNumber);
		log.info(
			"SafetyKorea certification number verification completed certNum={} candidateCount={}",
			maskCertificationNumber(certificationNumber),
			candidates.size()
		);

		SafetyKoreaCertificationItem matched = findMatchingCertification(certificationNumber, candidates);
		if (matched == null) {
			return new KcCertificationVerificationResult(
				KcCertificationVerificationStatus.INVALID,
				false,
				null,
				null,
				"입력된 KC 인증번호를 제품안전정보센터 인증 DB에서 확인하지 못했습니다."
			);
		}

		if (!isValidStatus(matched.statusName())) {
			return new KcCertificationVerificationResult(
				KcCertificationVerificationStatus.INVALID,
				false,
				matched.certificationType(),
				matched.relatedLaw(),
				"유효하지 않은 KC 인증입니다. 인증 상태가 취소, 만료, 폐기, 파기, 정지 또는 반납인지 확인하세요."
			);
		}

		return KcCertificationVerificationResult.valid(
			matched.certificationType(),
			matched.relatedLaw(),
			"제품안전정보센터에서 KC 인증번호가 유효한 인증으로 확인되었습니다."
		);
	}

	private SafetyKoreaCertificationItem findMatchingCertification(
		String certificationNumber,
		List<SafetyKoreaCertificationItem> candidates
	) {
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		String normalizedInput = normalizeCertificationNumber(certificationNumber);
		for (SafetyKoreaCertificationItem candidate : candidates) {
			if (candidate == null) {
				continue;
			}
			if (normalizedInput.equals(normalizeCertificationNumber(candidate.certificationNumber()))) {
				return candidate;
			}
		}
		return null;
	}

	private boolean isValidStatus(String statusName) {
		String normalized = statusName == null ? "" : statusName.replaceAll("\\s+", "").trim().toLowerCase();
		if (normalized.isBlank()) {
			return false;
		}
		return !normalized.contains("취소")
			&& !normalized.contains("만료")
			&& !normalized.contains("폐기")
			&& !normalized.contains("파기")
			&& !normalized.contains("정지")
			&& !normalized.contains("반납")
			&& !normalized.contains("invalid")
			&& !normalized.contains("expired")
			&& !normalized.contains("cancel");
	}

	private String normalizeCertificationNumber(String value) {
		return value == null ? "" : value.replaceAll("[\\s-]", "").trim().toUpperCase();
	}

	private String maskCertificationNumber(String certificationNumber) {
		if (!hasText(certificationNumber)) {
			return null;
		}
		String trimmed = certificationNumber.trim();
		if (trimmed.length() <= 4) {
			return "****";
		}
		return trimmed.substring(0, 2) + "****" + trimmed.substring(trimmed.length() - 2);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
