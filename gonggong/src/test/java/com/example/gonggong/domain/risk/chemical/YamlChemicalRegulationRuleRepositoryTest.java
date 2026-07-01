package com.example.gonggong.domain.risk.chemical;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class YamlChemicalRegulationRuleRepositoryTest {

	@Test
	void returnsOnlyEffectiveRulesAndOmitsUnverifiedPenalty() {
		String yaml = """
			rules:
			  - classificationIdentifier: "97-1-5"
			    classificationName: "유독물질"
			    regulated: true
			    lawName: "화학물질관리법"
			    obligationArticle: "제20조"
			    obligationSourceUrl: "https://www.law.go.kr/법령/화학물질관리법"
			    penaltyArticle: "제99조"
			    penaltyText: "검증되지 않은 문구"
			    penaltySourceUrl: "https://example.com/not-official"
			    effectiveFrom: 2025-08-07
			""";
		YamlChemicalRegulationRuleRepository repository = new YamlChemicalRegulationRuleRepository(
			new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8))
		);

		ChemicalRegulationRule rule = repository.findActive("97-1-5", LocalDate.of(2026, 7, 1)).orElseThrow();

		assertThat(rule.relatedLaw()).isEqualTo("화학물질관리법 제20조");
		assertThat(rule.verifiedPenalty()).isNull();
		assertThat(repository.findActive("97-1-5", LocalDate.of(2025, 1, 1))).isEmpty();
	}
}
