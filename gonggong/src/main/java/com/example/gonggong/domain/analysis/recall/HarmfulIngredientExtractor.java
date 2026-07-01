package com.example.gonggong.domain.analysis.recall;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HarmfulIngredientExtractor {

	private static final List<IngredientPattern> PATTERNS = List.of(
		new IngredientPattern("프탈레이트계 가소제", List.of("프탈레이트", "가소제")),
		new IngredientPattern("DEHP", List.of("DEHP")),
		new IngredientPattern("DBP", List.of("DBP")),
		new IngredientPattern("DINP", List.of("DINP")),
		new IngredientPattern("DIBP", List.of("DIBP")),
		new IngredientPattern("납", List.of("납", "lead")),
		new IngredientPattern("카드뮴", List.of("카드뮴", "cadmium")),
		new IngredientPattern("니켈", List.of("니켈", "nickel")),
		new IngredientPattern("폼알데하이드", List.of("formaldehyde", "폼알데하이드", "포름알데히드")),
		new IngredientPattern("비스페놀A", List.of("비스페놀", "BPA"))
	);

	public List<String> extract(String description) {
		if (description == null || description.isBlank()) {
			return List.of();
		}

		String normalized = description.toLowerCase();
		List<String> ingredients = new ArrayList<>();
		for (IngredientPattern pattern : PATTERNS) {
			if (pattern.matches(normalized)) {
				ingredients.add(pattern.name());
			}
		}
		return ingredients;
	}

	private record IngredientPattern(String name, List<String> keywords) {

		private boolean matches(String value) {
			for (String keyword : keywords) {
				if (value.contains(keyword.toLowerCase())) {
					return true;
				}
			}
			return false;
		}
	}
}
