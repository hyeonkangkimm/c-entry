package com.example.gonggong.domain.hsk.initializer;

import com.example.gonggong.domain.hsk.dataset.HskDatasetReader;
import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.repository.HskItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class HskDatasetInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(HskDatasetInitializer.class);
	private static final int BATCH_SIZE = 500;

	private final HskItemRepository repository;
	private final HskDatasetReader datasetReader;
	private final Resource datasetResource;
	private final boolean initialize;

	public HskDatasetInitializer(
		HskItemRepository repository,
		HskDatasetReader datasetReader,
		@Value("${hsk.dataset.resource:classpath:data/customs-hsk-items-20260101.xlsx}") Resource datasetResource,
		@Value("${hsk.dataset.initialize:true}") boolean initialize
	) {
		this.repository = repository;
		this.datasetReader = datasetReader;
		this.datasetResource = datasetResource;
		this.initialize = initialize;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!initialize) {
			return;
		}

		List<HskItem> items = datasetReader.read(datasetResource).stream()
			.map(row -> new HskItem(row.hskCode(), row.koreanName(), row.englishName(), row.displayName()))
			.toList();
		long existingCount = repository.count();
		if (existingCount == items.size() && repository.countByDisplayNameIsNull() == 0) {
			return;
		}

		if (existingCount > 0) {
			repository.deleteAllInBatch();
			repository.flush();
		}
		for (int start = 0; start < items.size(); start += BATCH_SIZE) {
			int end = Math.min(start + BATCH_SIZE, items.size());
			repository.saveAll(items.subList(start, end));
			repository.flush();
		}
		log.info("Official HSK dataset initialized itemCount={}", items.size());
	}
}
