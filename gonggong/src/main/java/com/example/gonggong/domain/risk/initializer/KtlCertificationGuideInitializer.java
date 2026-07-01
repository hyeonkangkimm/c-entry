package com.example.gonggong.domain.risk.initializer;

import com.example.gonggong.domain.risk.domain.KtlCertificationGuide;
import com.example.gonggong.domain.risk.repository.KtlCertificationGuideRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class KtlCertificationGuideInitializer implements CommandLineRunner {

	private static final String APPLICATION_URL = "https://customer.ktl.re.kr/web/contents/login.do";
	private static final String SOURCE_URL = "https://customer.ktl.re.kr/web/contents/K101010200.do";
	private static final String FEE = "제품 사양에 따라 별도 견적";
	private static final String DOCUMENTS = "[\"제품사용설명서(국문)\",\"회로도\",\"KC 표시사항 도안\",\"사업자등록증\",\"주요 부품 인증서 및 사양서\",\"시료(제품)\"]";
	private static final String TEST_ITEMS = "[\"적용 안전기준에 따른 전기적 안전 시험\",\"절연 내력 시험\",\"온도 상승 시험\",\"전자파 적합성 시험(해당 시)\"]";

	private final KtlCertificationGuideRepository repository;

	public KtlCertificationGuideInitializer(KtlCertificationGuideRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run(String... args) {
		List<KtlCertificationGuide> guides = List.of(
			guide(
				"ELECTRICAL_SAFETY_CERTIFICATION",
				"전기용품 안전인증",
				"전기용품 및 생활용품 안전관리법 제5조",
				"평균 45영업일"
			),
			guide(
				"ELECTRICAL_SAFETY_CONFIRMATION",
				"전기용품 안전확인",
				"전기용품 및 생활용품 안전관리법 제15조",
				"제품 시험 및 보완 여부에 따라 변동"
			),
			guide(
				"ELECTRICAL_SUPPLIER_CONFORMITY",
				"전기용품 공급자적합성확인",
				"전기용품 및 생활용품 안전관리법 제23조",
				"제품 시험 및 보완 여부에 따라 변동"
			)
		);

		for (KtlCertificationGuide guide : guides) {
			if (!repository.existsByCertificationTypeKey(guide.getCertificationTypeKey())) {
				repository.save(guide);
			}
		}
	}

	private KtlCertificationGuide guide(String key, String name, String legalBasis, String duration) {
		return new KtlCertificationGuide(
			key,
			name,
			null,
			legalBasis,
			TEST_ITEMS,
			DOCUMENTS,
			duration,
			FEE,
			APPLICATION_URL,
			SOURCE_URL,
			LocalDate.of(2026, 7, 2)
		);
	}
}
