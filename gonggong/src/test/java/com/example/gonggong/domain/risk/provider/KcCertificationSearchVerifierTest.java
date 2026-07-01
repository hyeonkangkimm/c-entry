package com.example.gonggong.domain.risk.provider;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KcCertificationSearchVerifierTest {

	@Test
	void returnsValidWhenCertificationNumberIsFoundAndActive() {
		KcCertificationSearchVerifier verifier = new KcCertificationSearchVerifier(
			new FakeClient(List.of(new SafetyKoreaCertificationItem(
				"HU10772-22022",
				"충전기",
				"SPE-N12PD1P20",
				"Maker",
				"적합",
				"방송통신기자재 적합성 평가 대상",
				"전파법"
			)))
		);

		KcCertificationVerificationResult result = verifier.verify(
			"HU10772-22022",
			"ignored",
			"ignored",
			"ignored"
		);

		assertThat(result.status()).isEqualTo(KcCertificationVerificationStatus.VALID);
		assertThat(result.valid()).isTrue();
		assertThat(result.certificationType()).isEqualTo("방송통신기자재 적합성 평가 대상");
		assertThat(result.relatedLaw()).isEqualTo("전파법");
	}

	@Test
	void returnsInvalidWhenCertificationNumberIsNotFound() {
		KcCertificationSearchVerifier verifier = new KcCertificationSearchVerifier(new FakeClient(List.of()));

		KcCertificationVerificationResult result = verifier.verify(
			"HU10772-22022",
			null,
			null,
			null
		);

		assertThat(result.status()).isEqualTo(KcCertificationVerificationStatus.INVALID);
		assertThat(result.valid()).isFalse();
		assertThat(result.message()).contains("인증 DB에서 확인하지 못했습니다");
	}

	@Test
	void returnsInvalidWhenCertificationStatusIsNotValid() {
		KcCertificationSearchVerifier verifier = new KcCertificationSearchVerifier(
			new FakeClient(List.of(new SafetyKoreaCertificationItem(
				"SU12345-67890",
				"전기 찜질기",
				"ABC-1",
				"Maker",
				"취소",
				"전기용품 안전확인",
				"전기용품 및 생활용품 안전관리법"
			)))
		);

		KcCertificationVerificationResult result = verifier.verify(
			"SU12345-67890",
			null,
			null,
			null
		);

		assertThat(result.status()).isEqualTo(KcCertificationVerificationStatus.INVALID);
		assertThat(result.message()).contains("유효하지 않은 KC 인증");
	}

	@Test
	void doesNotSearchByProductModelOrBrandWhenCertificationNumberIsMissing() {
		RecordingClient client = new RecordingClient();
		KcCertificationSearchVerifier verifier = new KcCertificationSearchVerifier(client);

		KcCertificationVerificationResult result = verifier.verify(null, "노트북", "N4000", "Azeyou");

		assertThat(result.status()).isEqualTo(KcCertificationVerificationStatus.UNKNOWN);
		assertThat(client.certificationNumberQueries).isEmpty();
		assertThat(client.productNameQueries).isEmpty();
		assertThat(client.modelNameQueries).isEmpty();
		assertThat(client.brandNameQueries).isEmpty();
	}

	private record FakeClient(List<SafetyKoreaCertificationItem> items) implements SafetyKoreaCertificationClient {

		@Override
		public List<SafetyKoreaCertificationItem> searchByCertificationNumber(String certificationNumber) {
			return items;
		}

		@Override
		public List<SafetyKoreaCertificationItem> searchByProductName(String productName) {
			return List.of();
		}

		@Override
		public List<SafetyKoreaCertificationItem> searchByModelName(String modelName) {
			return List.of();
		}

		@Override
		public List<SafetyKoreaCertificationItem> searchByBrandName(String brandName) {
			return List.of();
		}
	}

	private static class RecordingClient implements SafetyKoreaCertificationClient {
		private final List<String> certificationNumberQueries = new ArrayList<>();
		private final List<String> productNameQueries = new ArrayList<>();
		private final List<String> modelNameQueries = new ArrayList<>();
		private final List<String> brandNameQueries = new ArrayList<>();

		@Override
		public List<SafetyKoreaCertificationItem> searchByCertificationNumber(String certificationNumber) {
			certificationNumberQueries.add(certificationNumber);
			return List.of();
		}

		@Override
		public List<SafetyKoreaCertificationItem> searchByProductName(String productName) {
			productNameQueries.add(productName);
			return List.of();
		}

		@Override
		public List<SafetyKoreaCertificationItem> searchByModelName(String modelName) {
			modelNameQueries.add(modelName);
			return List.of();
		}

		@Override
		public List<SafetyKoreaCertificationItem> searchByBrandName(String brandName) {
			brandNameQueries.add(brandName);
			return List.of();
		}
	}
}
