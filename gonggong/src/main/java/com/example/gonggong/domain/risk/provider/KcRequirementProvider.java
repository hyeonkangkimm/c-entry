package com.example.gonggong.domain.risk.provider;

import java.util.Optional;

@FunctionalInterface
public interface KcRequirementProvider {

	Optional<KcRequirementResult> findRequirement(String hskCode, String productName, String productDescription);
}
