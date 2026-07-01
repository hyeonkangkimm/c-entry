package com.example.gonggong.domain.risk.initializer;

import com.example.gonggong.domain.risk.domain.HskKcRequirement;
import com.example.gonggong.domain.risk.repository.HskKcRequirementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HskKcRequirementInitializer implements CommandLineRunner {

	private final HskKcRequirementRepository repository;

	public HskKcRequirementInitializer(HskKcRequirementRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run(String... args) {
		List<HskKcRequirement> requirements = List.of(
			new HskKcRequirement("9503", true, "어린이제품 안전확인", "어린이제품 안전 특별법"),
			new HskKcRequirement("8516", true, "전기용품 안전확인", "전기용품 및 생활용품 안전관리법"),
			new HskKcRequirement("8509", true, "전기용품 안전확인", "전기용품 및 생활용품 안전관리법"),
			new HskKcRequirement("9405", true, "전기용품 안전확인 또는 공급자적합성확인", "전기용품 및 생활용품 안전관리법"),
			new HskKcRequirement("8543", true, "전기용품 안전확인 또는 전자파 적합성평가", "전기용품 및 생활용품 안전관리법")
		);

		for (HskKcRequirement requirement : requirements) {
			if (!repository.existsByHskCodePrefix(requirement.getHskCodePrefix())) {
				repository.save(requirement);
			}
		}
	}
}
