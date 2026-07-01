package com.example.gonggong.domain.risk.provider;

import com.example.gonggong.domain.risk.domain.KtlCertificationGuide;
import com.example.gonggong.domain.risk.dto.response.KtlCertificationGuideResponse;
import com.example.gonggong.domain.risk.repository.KtlCertificationGuideRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class JpaKtlCertificationGuideProvider implements KtlCertificationGuideProvider {

	private final KtlCertificationGuideRepository repository;
	private final ObjectMapper objectMapper;

	public JpaKtlCertificationGuideProvider(KtlCertificationGuideRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@Override
	public Optional<KtlCertificationGuideResponse> findByCertificationType(String certificationType) {
		String key = normalizeTypeKey(certificationType);
		if (key == null) {
			return Optional.empty();
		}
		return repository.findByCertificationTypeKeyAndActiveTrue(key)
			.filter(this::hasOfficialUrls)
			.flatMap(this::toResponse);
	}

	String normalizeTypeKey(String value) {
		String normalized = value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
		if (!normalized.contains("전기용품")) {
			return null;
		}
		if (normalized.contains("공급자적합성확인")) {
			return "ELECTRICAL_SUPPLIER_CONFORMITY";
		}
		if (normalized.contains("안전확인")) {
			return "ELECTRICAL_SAFETY_CONFIRMATION";
		}
		if (normalized.contains("안전인증")) {
			return "ELECTRICAL_SAFETY_CERTIFICATION";
		}
		return null;
	}

	private Optional<KtlCertificationGuideResponse> toResponse(KtlCertificationGuide guide) {
		try {
			List<String> testItems = objectMapper.readValue(guide.getTestItemsJson(), new TypeReference<>() {});
			List<String> documents = objectMapper.readValue(guide.getRequiredDocumentsJson(), new TypeReference<>() {});
			return Optional.of(new KtlCertificationGuideResponse(
				guide.getCertificationName(),
				guide.getCertificationMarkUrl(),
				guide.getLegalBasis(),
				List.copyOf(testItems),
				List.copyOf(documents),
				guide.getEstimatedDuration(),
				guide.getEstimatedFee(),
				guide.getApplicationUrl(),
				actionGuide(guide.getCertificationName(), documents),
				guide.getSourceUrl(),
				guide.getVerifiedAt()
			));
		}
		catch (Exception ignored) {
			return Optional.empty();
		}
	}

	private String actionGuide(String certificationName, List<String> documents) {
		String document = documents.stream()
			.filter(value -> value != null && !value.isBlank())
			.findFirst()
			.orElse("필수 서류");
		return document + " 등 준비 서류를 확인한 후 KTL에서 " + certificationName + " 시험을 신청하세요.";
	}

	private boolean hasOfficialUrls(KtlCertificationGuide guide) {
		return isKtlUrl(guide.getApplicationUrl())
			&& isKtlUrl(guide.getSourceUrl())
			&& (guide.getCertificationMarkUrl() == null
				|| isKtlUrl(guide.getCertificationMarkUrl())
				|| isSafetyKoreaUrl(guide.getCertificationMarkUrl()));
	}

	private boolean isKtlUrl(String value) {
		return hasOfficialHost(value, "ktl.re.kr");
	}

	private boolean isSafetyKoreaUrl(String value) {
		return hasOfficialHost(value, "safetykorea.kr");
	}

	private boolean hasOfficialHost(String value, String officialHost) {
		try {
			URI uri = URI.create(value);
			String host = uri.getHost();
			return "https".equalsIgnoreCase(uri.getScheme())
				&& host != null
				&& (host.equalsIgnoreCase(officialHost) || host.toLowerCase(Locale.ROOT).endsWith("." + officialHost));
		}
		catch (Exception ignored) {
			return false;
		}
	}
}
