package com.example.gonggong.domain.risk.chemical;

import java.time.LocalDate;
import java.util.Optional;

@FunctionalInterface
public interface ChemicalRegulationRuleRepository {
	Optional<ChemicalRegulationRule> findActive(String classificationIdentifier, LocalDate date);
}
