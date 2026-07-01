package com.example.gonggong.domain.risk.provider;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HttpCustomsConfirmationClientTest {

	@Test
	void mapsKcRelatedCustomsConfirmationItemToRequirement() {
		CustomsConfirmationProperties properties = new CustomsConfirmationProperties();
		HttpCustomsConfirmationClient client = new HttpCustomsConfirmationClient(
			null,
			properties,
			new CustomsConfirmationXmlParser()
		);

		Optional<KcRequirementResult> result = client.toRequirement("9503003910", java.util.List.of(
			new CustomsConfirmationItem(
				"9503003910",
				"KC01",
				"어린이제품 안전 특별법",
				null,
				"국가기술표준원",
				"20260101"
			)
		));

		assertThat(result).isPresent();
		assertThat(result.get().certificationRequired()).isTrue();
		assertThat(result.get().certificationType()).isEqualTo("어린이제품 안전확인");
		assertThat(result.get().relatedLaw()).isEqualTo("어린이제품 안전 특별법");
		assertThat(result.get().approvalAgency()).isEqualTo("국가기술표준원");
		assertThat(result.get().source()).isEqualTo("customs-confirmation-api");
	}

	@Test
	void returnsEmptyWhenCustomsConfirmationHasNoKcRelatedLaw() {
		CustomsConfirmationProperties properties = new CustomsConfirmationProperties();
		HttpCustomsConfirmationClient client = new HttpCustomsConfirmationClient(
			null,
			properties,
			new CustomsConfirmationXmlParser()
		);

		Optional<KcRequirementResult> result = client.toRequirement("0207142090", java.util.List.of(
			new CustomsConfirmationItem(
				"0207142090",
				"FOOD01",
				"수입식품안전관리 특별법",
				null,
				"식품의약품안전처",
				"20260101"
			)
		));

		assertThat(result).isEmpty();
	}
}
