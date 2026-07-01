package com.example.gonggong.domain.hsk.repository;

import com.example.gonggong.domain.hsk.domain.HskItem;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaHskItemReader implements HskItemReader {

	private static final int SEARCH_LIMIT = 200;

	private final HskItemRepository repository;

	public JpaHskItemReader(HskItemRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<HskItem> findCandidates(String keyword) {
		return repository.searchByName(keyword, PageRequest.of(0, SEARCH_LIMIT));
	}
}
