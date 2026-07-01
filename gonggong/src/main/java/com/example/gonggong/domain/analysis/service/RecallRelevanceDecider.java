package com.example.gonggong.domain.analysis.service;

import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallItem;

import java.util.List;

public interface RecallRelevanceDecider {

	List<SafetyKoreaRecallItem> selectRelevant(ProductNormalizeResult normalized, List<SafetyKoreaRecallItem> candidates);

	static RecallRelevanceDecider keepAll() {
		return (normalized, candidates) -> candidates == null ? List.of() : candidates;
	}
}
