package com.example.gonggong.domain.risk.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Primary
@Component
public class CompositeKcRequirementProvider implements KcRequirementProvider {

	private static final Logger log = LoggerFactory.getLogger(CompositeKcRequirementProvider.class);

	private final CustomsConfirmationClient customsConfirmationClient;
	private final JpaKcRequirementProvider fallbackProvider;

	public CompositeKcRequirementProvider(
		CustomsConfirmationClient customsConfirmationClient,
		JpaKcRequirementProvider fallbackProvider
	) {
		this.customsConfirmationClient = customsConfirmationClient;
		this.fallbackProvider = fallbackProvider;
	}

	@Override
	public Optional<KcRequirementResult> findRequirement(
		String hskCode,
		String productName,
		String productDescription
	) {
		Optional<KcRequirementResult> customsRequirement = customsConfirmationClient.findImportRequirement(hskCode);
		if (customsRequirement.isPresent()) {
			KcRequirementResult result = customsRequirement.get();
			log.info(
				"KC requirement resolved by customs confirmation hskCode={} certificationRequired={} certificationType={} relatedLaw={} approvalAgency={}",
				hskCode,
				result.certificationRequired(),
				result.certificationType(),
				result.relatedLaw(),
				result.approvalAgency()
			);
			return customsRequirement;
		}

		Optional<KcRequirementResult> fallbackRequirement = fallbackProvider.findRequirement(
			hskCode,
			productName,
			productDescription
		);
		fallbackRequirement.ifPresent(result -> log.info(
			"KC requirement resolved by local fallback hskCode={} certificationRequired={} certificationType={} relatedLaw={}",
			hskCode,
			result.certificationRequired(),
			result.certificationType(),
			result.relatedLaw()
		));
		return fallbackRequirement;
	}
}
