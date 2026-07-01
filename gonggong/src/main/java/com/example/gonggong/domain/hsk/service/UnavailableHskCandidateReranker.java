package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnavailableHskCandidateReranker implements HskCandidateReranker {

	@Override
	public HskRerankResult rerank(HskMatchRequest request, HskFeatures features, List<HskVectorCandidate> candidates) {
		return null;
	}
}
