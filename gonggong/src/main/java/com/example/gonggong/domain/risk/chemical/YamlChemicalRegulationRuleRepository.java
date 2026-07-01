package com.example.gonggong.domain.risk.chemical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class YamlChemicalRegulationRuleRepository implements ChemicalRegulationRuleRepository {

	private final List<ChemicalRegulationRule> rules;

	public YamlChemicalRegulationRuleRepository() {
		this(new ClassPathResource("data/chemical-regulation-rules.yaml"));
	}

	YamlChemicalRegulationRuleRepository(Resource resource) {
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
			RuleDocument document = mapper.readValue(resource.getInputStream(), RuleDocument.class);
			this.rules = document.rules() == null ? List.of() : document.rules().stream()
				.filter(this::valid)
				.toList();
		}
		catch (IOException exception) {
			throw new IllegalStateException("Chemical regulation rules could not be loaded", exception);
		}
	}

	@Override
	public Optional<ChemicalRegulationRule> findActive(String classificationIdentifier, LocalDate date) {
		if (classificationIdentifier == null || date == null) return Optional.empty();
		return rules.stream()
			.filter(rule -> classificationIdentifier.equals(rule.classificationIdentifier()))
			.filter(rule -> rule.activeOn(date))
			.findFirst();
	}

	private boolean valid(ChemicalRegulationRule rule) {
		return rule != null
			&& rule.classificationIdentifier() != null
			&& rule.obligationSourceUrl() != null
			&& rule.obligationSourceUrl().startsWith("https://www.law.go.kr/");
	}

	private record RuleDocument(List<ChemicalRegulationRule> rules) {
	}
}
