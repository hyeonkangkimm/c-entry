package com.example.gonggong.domain.risk.provider;

import com.example.gonggong.domain.risk.domain.HskKcRequirement;
import com.example.gonggong.domain.risk.repository.HskKcRequirementRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

@Component
public class JpaKcRequirementProvider implements KcRequirementProvider {

	private final HskKcRequirementRepository repository;

	public JpaKcRequirementProvider(HskKcRequirementRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<KcRequirementResult> findRequirement(
		String hskCode,
		String productName,
		String productDescription
	) {
		if (hskCode == null || hskCode.isBlank()) {
			return Optional.empty();
		}
		String normalizedCode = hskCode.trim();
		return repository.findByActiveTrue().stream()
			.filter(requirement -> normalizedCode.startsWith(requirement.getHskCodePrefix()))
			.sorted(Comparator.comparingInt((HskKcRequirement requirement) -> requirement.getHskCodePrefix().length()).reversed())
			.findFirst()
			.map(requirement -> new KcRequirementResult(
				requirement.isCertificationRequired(),
				requirement.getCertificationType(),
				requirement.getRelatedLaw(),
				null,
				"local-hsk-kc-requirement"
			));
	}
}
