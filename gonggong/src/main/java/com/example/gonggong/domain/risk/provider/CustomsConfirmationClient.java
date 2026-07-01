package com.example.gonggong.domain.risk.provider;

import java.util.Optional;

public interface CustomsConfirmationClient {

	Optional<KcRequirementResult> findImportRequirement(String hskCode);
}
