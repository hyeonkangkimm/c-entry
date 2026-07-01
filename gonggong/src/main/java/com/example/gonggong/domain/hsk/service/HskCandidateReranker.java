package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.dto.HskMatchRequest;

import java.util.List;

@FunctionalInterface
public interface HskCandidateReranker {

	HskRerankResult rerank(HskMatchRequest request, HskFeatures features, List<HskVectorCandidate> candidates);

	static HskCandidateReranker keepVectorOrder() {
		return (request, features, candidates) -> null;
	}
}
