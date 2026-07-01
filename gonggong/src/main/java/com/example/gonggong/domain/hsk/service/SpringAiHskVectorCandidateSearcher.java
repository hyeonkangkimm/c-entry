package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.example.gonggong.domain.hsk.repository.HskItemRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Primary
public class SpringAiHskVectorCandidateSearcher implements HskVectorCandidateSearcher {

	private static final Logger log = LoggerFactory.getLogger(SpringAiHskVectorCandidateSearcher.class);

	private final ObjectProvider<VectorStore> vectorStoreProvider;
	private final HskItemRepository repository;
	private final HskVectorQueryBuilder queryBuilder;

	public SpringAiHskVectorCandidateSearcher(
		ObjectProvider<VectorStore> vectorStoreProvider,
		HskItemRepository repository,
		HskVectorQueryBuilder queryBuilder
	) {
		this.vectorStoreProvider = vectorStoreProvider;
		this.repository = repository;
		this.queryBuilder = queryBuilder;
	}

	@Override
	@Transactional(readOnly = true)
	public List<HskVectorCandidate> search(HskMatchRequest request, HskFeatures features, int limit) {
		VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
		if (vectorStore == null) {
			return List.of();
		}
		String query = queryBuilder.buildQuery(request, features);
		if (query.isBlank()) {
			return List.of();
		}
		log.info("HSK vector search started query={} limit={}", abbreviate(query), limit);
		List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
			.query(query)
			.topK(limit)
			.build());
		List<String> hskCodes = documents.stream()
			.map(document -> metadataValue(document, "hskCode"))
			.filter(value -> value != null && !value.isBlank())
			.distinct()
			.toList();
		log.info(
			"HSK vector search completed resultCount={} topHskCodes={} query={}",
			hskCodes.size(),
			hskCodes.stream().limit(5).toList(),
			abbreviate(query)
		);
		if (hskCodes.isEmpty()) {
			return List.of();
		}

		Map<String, HskItem> itemsByCode = new LinkedHashMap<>();
		repository.findByHskCodeIn(hskCodes)
			.forEach(item -> itemsByCode.put(item.getHskCode(), item));
		Map<String, Double> similarityByCode = new LinkedHashMap<>();
		double score = 0.90;
		for (String hskCode : hskCodes) {
			similarityByCode.putIfAbsent(hskCode, score);
			score = Math.max(0.50, score - 0.02);
		}
		return hskCodes.stream()
			.map(itemsByCode::get)
			.filter(item -> item != null)
			.map(item -> new HskVectorCandidate(item, similarityByCode.getOrDefault(item.getHskCode(), 0.50)))
			.toList();
	}

	private String metadataValue(Document document, String key) {
		Object value = document.getMetadata().get(key);
		return value == null ? null : value.toString();
	}

	private String abbreviate(String value) {
		String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
		if (normalized.length() <= 160) {
			return normalized;
		}
		return normalized.substring(0, 157) + "...";
	}
}
