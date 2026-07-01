package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.dto.HskMatchRequest;

import java.util.List;

@FunctionalInterface
public interface HskVectorCandidateSearcher {

	List<HskVectorCandidate> search(HskMatchRequest request, HskFeatures features, int limit);
}
