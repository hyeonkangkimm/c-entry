package com.example.gonggong.domain.hsk.repository;

import com.example.gonggong.domain.hsk.domain.HskItem;

import java.util.List;

@FunctionalInterface
public interface HskItemReader {

	List<HskItem> findCandidates(String keyword);
}
