package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.KcRiskResponse;
import com.example.gonggong.domain.risk.provider.KcCertificationVerificationResult;
import com.example.gonggong.domain.risk.provider.KcCertificationVerificationStatus;
import com.example.gonggong.domain.risk.provider.KcCertificationVerifier;
import com.example.gonggong.domain.risk.provider.KcRequirementProvider;
import com.example.gonggong.domain.risk.provider.KcRequirementResult;
import com.example.gonggong.domain.risk.provider.KtlCertificationGuideProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KcRiskService {

	private final KcRequirementProvider requirementProvider;
	private final KcCertificationVerifier certificationVerifier;
	private final KtlCertificationGuideProvider ktlCertificationGuideProvider;
	private final String safetyKoreaUrl;

	@Autowired
	public KcRiskService(
		KcRequirementProvider requirementProvider,
		KcCertificationVerifier certificationVerifier,
		KtlCertificationGuideProvider ktlCertificationGuideProvider,
		@Value("${external-links.product-safety-center-url:https://www.safetykorea.kr/}") String safetyKoreaUrl
	) {
		this.requirementProvider = requirementProvider;
		this.certificationVerifier = certificationVerifier;
		this.ktlCertificationGuideProvider = ktlCertificationGuideProvider;
		this.safetyKoreaUrl = safetyKoreaUrl;
	}

	public KcRiskService(
		KcRequirementProvider requirementProvider,
		KcCertificationVerifier certificationVerifier,
		String safetyKoreaUrl
	) {
		this(requirementProvider, certificationVerifier, certificationType -> java.util.Optional.empty(), safetyKoreaUrl);
	}

	public KcRiskResponse analyze(RiskDashboardAnalyzeRequest request) {
		KcRequirementResult requirement = requirementProvider
			.findRequirement(request.hskCode(), request.productName(), request.productDescription())
			.orElse(null);

		String certificationNumber = request.kcCertificationNumber();
		if (!hasText(certificationNumber)) {
			return new KcRiskResponse(
				RiskStatus.DANGER,
				90,
				true,
				false,
				null,
				firstNonBlank(request.normalizedKcCertificationType(), certificationType(requirement)),
				relatedLaw(requirement),
				"판매 페이지에서 KC 인증번호를 확인하지 못했습니다. KC 인증이 확인되지 않은 상품으로 판단합니다.",
				safetyKoreaUrl,
				"제품안전정보센터에서 실시간 검증하기"
			);
		}

		KcCertificationVerificationResult verification = certificationVerifier.verify(
			certificationNumber,
			null,
			null,
			null,
			List.of()
		);
		com.example.gonggong.domain.risk.dto.response.KtlCertificationGuideResponse ktlGuide = verification.valid()
			? ktlCertificationGuideProvider.findByCertificationType(
				String.join(" ",
					verification.certificationType() == null ? "" : verification.certificationType(),
					verification.relatedLaw() == null ? "" : verification.relatedLaw()
				)
			).orElse(null)
			: null;

		return new KcRiskResponse(
			mapVerificationStatus(verification.status()),
			mapVerificationScore(verification.status()),
			true,
			verification.valid(),
			maskCertificationNumber(certificationNumber),
			firstNonBlank(verification.certificationType(), request.normalizedKcCertificationType(), certificationType(requirement)),
			firstNonBlank(relatedLaw(requirement), verification.relatedLaw()),
			firstNonBlank(verification.message(), defaultVerificationMessage(verification.status())),
			safetyKoreaUrl,
			"제품안전정보센터에서 실시간 검증하기",
			ktlGuide
		);
	}

	private String certificationType(KcRequirementResult requirement) {
		return requirement == null ? null : requirement.certificationType();
	}

	private String relatedLaw(KcRequirementResult requirement) {
		return requirement == null ? null : requirement.relatedLaw();
	}

	private RiskStatus mapVerificationStatus(KcCertificationVerificationStatus status) {
		return switch (status) {
			case VALID -> RiskStatus.SAFE;
			case INVALID -> RiskStatus.DANGER;
			case FOUND_CANDIDATE -> RiskStatus.WARNING;
			case UNKNOWN -> RiskStatus.UNKNOWN;
			case UNAVAILABLE -> RiskStatus.UNAVAILABLE;
		};
	}

	private int mapVerificationScore(KcCertificationVerificationStatus status) {
		return switch (status) {
			case VALID -> 15;
			case INVALID -> 90;
			case FOUND_CANDIDATE -> 45;
			case UNKNOWN -> 50;
			case UNAVAILABLE -> 50;
		};
	}

	private String defaultVerificationMessage(KcCertificationVerificationStatus status) {
		return switch (status) {
			case VALID -> "제품안전정보센터에서 KC 인증번호가 확인되었습니다.";
			case INVALID -> "입력된 KC 인증번호를 제품안전정보센터에서 유효한 인증으로 확인하지 못했습니다.";
			case FOUND_CANDIDATE -> "KC 인증 후보가 확인되었으나 동일 모델 여부는 직접 확인이 필요합니다.";
			case UNKNOWN -> "KC 인증번호 확인 결과를 판단할 수 없습니다.";
			case UNAVAILABLE -> "제품안전정보센터 KC 인증 정보를 현재 조회할 수 없습니다.";
		};
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

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (hasText(value)) {
				return value.trim();
			}
		}
		return null;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
