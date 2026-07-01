package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnavailableHskVectorCandidateSearcher implements HskVectorCandidateSearcher {

	@Override
	public List<HskVectorCandidate> search(HskMatchRequest request, HskFeatures features, int limit) {
		return List.of();
	}
}
