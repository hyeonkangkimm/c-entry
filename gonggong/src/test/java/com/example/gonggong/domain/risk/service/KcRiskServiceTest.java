package com.example.gonggong.domain.risk.service;

import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.KcRiskResponse;
import com.example.gonggong.domain.risk.provider.KcCertificationVerificationResult;
import com.example.gonggong.domain.risk.provider.KcCertificationVerificationStatus;
import com.example.gonggong.domain.risk.provider.KcCertificationVerifier;
import com.example.gonggong.domain.risk.provider.KcRequirementProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class KcRiskServiceTest {

	private static final String SAFETY_KOREA_URL = "https://www.safetykorea.kr/";

	@Test
	void marksDangerWhenCertificationNumberIsMissing() {
		KcRiskService service = service((certificationNumber, productName, modelName, brandName, keywords) ->
			KcCertificationVerificationResult.valid("전기용품 안전확인", "전기용품 및 생활용품 안전관리법", "should not be called")
		);

		KcRiskResponse response = service.analyze(request(null, "방송통신기자재 적합성 평가 대상"));

		assertThat(response.status()).isEqualTo(RiskStatus.DANGER);
		assertThat(response.score()).isEqualTo(90);
		assertThat(response.certificationRequired()).isTrue();
		assertThat(response.certificationValid()).isFalse();
		assertThat(response.certificationNumber()).isNull();
		assertThat(response.certificationType()).isEqualTo("방송통신기자재 적합성 평가 대상");
		assertThat(response.message()).contains("KC 인증번호를 확인하지 못했습니다", "KC 인증이 확인되지 않은 상품");
	}

	@Test
	void verifiesOnlyByCertificationNumberWhenCertificationNumberExists() {
		AtomicReference<String> requestedCertificationNumber = new AtomicReference<>();
		KcRiskService service = service((certificationNumber, productName, modelName, brandName, keywords) -> {
			requestedCertificationNumber.set(certificationNumber);
			return KcCertificationVerificationResult.valid(
				"방송통신기자재 적합성 평가 대상",
				"전파법",
				"제품안전정보센터에서 KC 인증번호가 유효한 인증으로 확인되었습니다."
			);
		});

		KcRiskResponse response = service.analyze(request("HU10772-22022", "KC 인증"));

		assertThat(requestedCertificationNumber.get()).isEqualTo("HU10772-22022");
		assertThat(response.status()).isEqualTo(RiskStatus.SAFE);
		assertThat(response.certificationValid()).isTrue();
		assertThat(response.certificationNumber()).isEqualTo("HU****22");
		assertThat(response.certificationType()).isEqualTo("방송통신기자재 적합성 평가 대상");
		assertThat(response.relatedLaw()).isEqualTo("전파법");
	}

	@Test
	void marksDangerWhenCertificationNumberIsRejectedByCertificationDb() {
		KcRiskService service = service((certificationNumber, productName, modelName, brandName, keywords) ->
			new KcCertificationVerificationResult(
				KcCertificationVerificationStatus.INVALID,
				false,
				null,
				null,
				"입력된 KC 인증번호를 제품안전정보센터 인증 DB에서 확인하지 못했습니다."
			)
		);

		KcRiskResponse response = service.analyze(request("HU10772-22022", null));

		assertThat(response.status()).isEqualTo(RiskStatus.DANGER);
		assertThat(response.certificationValid()).isFalse();
		assertThat(response.certificationNumber()).isEqualTo("HU****22");
		assertThat(response.message()).contains("인증 DB에서 확인하지 못했습니다");
	}

	@Test
	void returnsUnavailableWhenCertificationDbCannotBeQueried() {
		KcRiskService service = service((certificationNumber, productName, modelName, brandName, keywords) ->
			KcCertificationVerificationResult.unavailable("제품안전정보센터 KC 인증 정보를 현재 조회할 수 없습니다.")
		);

		KcRiskResponse response = service.analyze(request("HU10772-22022", null));

		assertThat(response.status()).isEqualTo(RiskStatus.UNAVAILABLE);
		assertThat(response.certificationValid()).isFalse();
		assertThat(response.certificationNumber()).isEqualTo("HU****22");
	}

	private KcRiskService service(KcCertificationVerifier verifier) {
		KcRequirementProvider unusedProvider = (hskCode, productName, description) -> Optional.empty();
		return new KcRiskService(unusedProvider, verifier, SAFETY_KOREA_URL);
	}

	private RiskDashboardAnalyzeRequest request(String certificationNumber, String certificationType) {
		return new RiskDashboardAnalyzeRequest(
			"8504403010",
			"18W 고속 충전기",
			"KC 인증 번호 HU10772-22022 방송통신기자재 적합성 평가 대상",
			List.of(),
			"CN",
			new BigDecimal("10000"),
			"KRW",
			1,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			certificationNumber,
			certificationType,
			null,
			null,
			"충전기",
			"충전기",
			List.of("충전기"),
			List.of("충전기"),
			List.of()
		);
	}
}
