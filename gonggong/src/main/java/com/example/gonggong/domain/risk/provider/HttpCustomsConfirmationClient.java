package com.example.gonggong.domain.risk.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Primary
@Component
public class HttpCustomsConfirmationClient implements CustomsConfirmationClient {

	private static final Logger log = LoggerFactory.getLogger(HttpCustomsConfirmationClient.class);

	private final RestClient restClient;
	private final CustomsConfirmationProperties properties;
	private final CustomsConfirmationXmlParser parser;

	public HttpCustomsConfirmationClient(
		RestClient.Builder restClientBuilder,
		CustomsConfirmationProperties properties,
		CustomsConfirmationXmlParser parser
	) {
		this.restClient = restClientBuilder == null ? null : restClientBuilder.build();
		this.properties = properties;
		this.parser = parser;
	}

	@Override
	public Optional<KcRequirementResult> findImportRequirement(String hskCode) {
		if (hskCode == null || hskCode.isBlank()) {
			return Optional.empty();
		}
		if (!properties.enabled() || restClient == null) {
			log.info("Customs confirmation API skipped hskCode={} reason=missing-api-url-or-service-key", hskCode);
			return Optional.empty();
		}

		try {
			log.info("Customs confirmation API request started hskCode={}", hskCode);
			URI uri = UriComponentsBuilder.fromUriString(properties.getApiUrl())
				.queryParam(properties.getServiceKeyParamName(), properties.getServiceKey())
				.queryParam(properties.getHskCodeParamName(), hskCode.trim())
				.queryParam(properties.getImportExportCodeParamName(), properties.getImportCode())
				.build(true)
				.toUri();
			String body = restClient.get()
				.uri(uri)
				.retrieve()
				.body(String.class);
			List<CustomsConfirmationItem> items = parser.parse(body);
			log.info("Customs confirmation API parsed hskCode={} itemCount={}", hskCode, items.size());
			return toRequirement(hskCode, items);
		}
		catch (Exception exception) {
			log.warn("Customs confirmation API failed hskCode={} error={}", hskCode, exception.getMessage());
			return Optional.empty();
		}
	}

	Optional<KcRequirementResult> toRequirement(String hskCode, List<CustomsConfirmationItem> items) {
		if (items == null || items.isEmpty()) {
			return Optional.empty();
		}
		return items.stream()
			.filter(item -> item != null && isKcRelatedLaw(item.lawName()))
			.findFirst()
			.map(item -> new KcRequirementResult(
				true,
				certificationType(item.lawName()),
				item.lawName(),
				item.approvalAgencyName(),
				"customs-confirmation-api"
			));
	}

	private boolean isKcRelatedLaw(String lawName) {
		String normalized = normalize(lawName);
		return normalized.contains("어린이제품")
			|| normalized.contains("전기용품")
			|| normalized.contains("생활용품")
			|| normalized.contains("제품안전")
			|| normalized.contains("전파법")
			|| normalized.contains("전기통신사업법");
	}

	private String certificationType(String lawName) {
		String normalized = normalize(lawName);
		if (normalized.contains("어린이제품")) {
			return "어린이제품 안전확인";
		}
		if (normalized.contains("전기용품") || normalized.contains("생활용품")) {
			return "전기용품 또는 생활용품 KC 인증";
		}
		if (normalized.contains("전파법") || normalized.contains("전기통신사업법")) {
			return "방송통신기자재 적합성평가";
		}
		return "KC 인증 또는 수입요건 확인";
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
