package com.example.gonggong.domain.hsk.initializer;

import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.repository.HskItemRepository;
import com.example.gonggong.domain.hsk.service.HskVectorQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@DependsOn("hskDatasetInitializer")
public class HskVectorStoreInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(HskVectorStoreInitializer.class);

	private final ObjectProvider<VectorStore> vectorStoreProvider;
	private final JdbcTemplate jdbcTemplate;
	private final HskItemRepository repository;
	private final HskVectorQueryBuilder queryBuilder;
	private final boolean initialize;
	private final int batchSize;

	public HskVectorStoreInitializer(
		ObjectProvider<VectorStore> vectorStoreProvider,
		JdbcTemplate jdbcTemplate,
		HskItemRepository repository,
		HskVectorQueryBuilder queryBuilder,
		@Value("${hsk.embedding.initialize:true}") boolean initialize,
		@Value("${hsk.embedding.batch-size:100}") int batchSize
	) {
		this.vectorStoreProvider = vectorStoreProvider;
		this.jdbcTemplate = jdbcTemplate;
		this.repository = repository;
		this.queryBuilder = queryBuilder;
		this.initialize = initialize;
		this.batchSize = Math.max(1, batchSize);
	}

	@Override
	public void run(String... args) {
		if (!initialize) {
			return;
		}
		VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
		if (vectorStore == null) {
			log.warn("HSK vector store initialization skipped because VectorStore bean is unavailable");
			return;
		}
		long itemCount = repository.count();
		long embeddedCount = embeddedHskCount();
		if (itemCount > 0 && embeddedCount == itemCount) {
			log.info("HSK vector store already initialized itemCount={} documentVersion={}", embeddedCount, HskVectorQueryBuilder.DOCUMENT_VERSION);
			return;
		}
		if (itemCount > 0) {
			int deletedCount = jdbcTemplate.update("delete from hsk_vector_store where metadata ->> 'domain' = 'hsk'");
			log.info(
				"HSK vector store refresh started itemCount={} embeddedCount={} deletedCount={} documentVersion={}",
				itemCount,
				embeddedCount,
				deletedCount,
				HskVectorQueryBuilder.DOCUMENT_VERSION
			);
		}
		List<HskItem> items = repository.findAll();
		for (int start = 0; start < items.size(); start += batchSize) {
			int end = Math.min(start + batchSize, items.size());
			vectorStore.add(items.subList(start, end).stream()
				.map(this::toDocument)
				.toList());
			log.info("HSK vector embedding batch saved start={} end={} total={}", start, end, items.size());
		}
		log.info("HSK vector store initialized itemCount={}", items.size());
	}

	private long embeddedHskCount() {
		try {
			Integer count = jdbcTemplate.queryForObject(
				"select count(*) from hsk_vector_store where metadata ->> 'domain' = 'hsk' and metadata ->> 'documentVersion' = ?",
				Integer.class
				,
				String.valueOf(HskVectorQueryBuilder.DOCUMENT_VERSION)
			);
			return count == null ? 0 : count;
		} catch (DataAccessException exception) {
			return 0;
		}
	}

	private Document toDocument(HskItem item) {
		return new Document(queryBuilder.buildDocumentText(item), Map.of(
			"domain", "hsk",
			"hskCode", item.getHskCode(),
			"koreanName", item.getKoreanName(),
			"displayName", item.getDisplayName(),
			"englishName", item.getEnglishName(),
			"documentVersion", String.valueOf(HskVectorQueryBuilder.DOCUMENT_VERSION)
		));
	}
}
