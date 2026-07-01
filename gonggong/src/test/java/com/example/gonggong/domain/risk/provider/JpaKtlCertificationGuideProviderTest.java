package com.example.gonggong.domain.risk.provider;

import com.example.gonggong.domain.risk.domain.KtlCertificationGuide;
import com.example.gonggong.domain.risk.dto.response.KtlCertificationGuideResponse;
import com.example.gonggong.domain.risk.repository.KtlCertificationGuideRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JpaKtlCertificationGuideProviderTest {

	@Test
	void resolvesElectricalSafetyCertificationFromSafetyKoreaType() {
		KtlCertificationGuideRepository repository = mock(KtlCertificationGuideRepository.class);
		KtlCertificationGuide guide = new KtlCertificationGuide(
			"ELECTRICAL_SAFETY_CERTIFICATION",
			"전기용품 안전인증",
			"https://www.safetykorea.kr/",
			"전기용품 및 생활용품 안전관리법 제5조",
			"[\"절연 내력 시험\",\"온도 상승 시험\"]",
			"[\"사업자등록증\",\"제품설명서\",\"회로도\"]",
			"평균 45영업일",
			"제품 사양에 따라 별도 견적",
			"https://customer.ktl.re.kr/web/contents/login.do",
			"https://customer.ktl.re.kr/web/contents/K101010200.do",
			LocalDate.of(2026, 7, 2)
		);
		when(repository.findByCertificationTypeKeyAndActiveTrue("ELECTRICAL_SAFETY_CERTIFICATION"))
			.thenReturn(Optional.of(guide));

		JpaKtlCertificationGuideProvider provider = new JpaKtlCertificationGuideProvider(repository, new ObjectMapper());

		Optional<KtlCertificationGuideResponse> result = provider.findByCertificationType("KC 전기용품 안전인증");

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().certificationName()).isEqualTo("전기용품 안전인증");
		assertThat(result.orElseThrow().testItems()).containsExactly("절연 내력 시험", "온도 상승 시험");
		assertThat(result.orElseThrow().actionItemGuide()).contains("제품설명서", "KTL");
	}

	@Test
	void skipsUnknownCertificationTypeWithoutQueryingRepository() {
		KtlCertificationGuideRepository repository = mock(KtlCertificationGuideRepository.class);
		JpaKtlCertificationGuideProvider provider = new JpaKtlCertificationGuideProvider(repository, new ObjectMapper());

		assertThat(provider.findByCertificationType("확인되지 않은 인증")).isEmpty();
		verifyNoInteractions(repository);
	}
}
