package com.example.gonggong.domain.demand.initializer;

import com.example.gonggong.domain.demand.entity.EssentialIndustryItem;
import com.example.gonggong.domain.demand.entity.ImportTrend;
import com.example.gonggong.domain.demand.repository.EssentialIndustryItemRepository;
import com.example.gonggong.domain.demand.repository.ImportTrendRepository;
import com.example.gonggong.global.exception.DataInitializationException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DemandSampleDataInitializer implements CommandLineRunner {

	private final ImportTrendRepository importTrendRepository;
	private final EssentialIndustryItemRepository essentialIndustryItemRepository;

	public DemandSampleDataInitializer(
		ImportTrendRepository importTrendRepository,
		EssentialIndustryItemRepository essentialIndustryItemRepository
	) {
		this.importTrendRepository = importTrendRepository;
		this.essentialIndustryItemRepository = essentialIndustryItemRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		try {
			if (importTrendRepository.count() == 0) {
				importTrendRepository.saveAll(sampleImportTrends());
			}
			if (essentialIndustryItemRepository.count() == 0) {
				essentialIndustryItemRepository.saveAll(sampleEssentialItems());
			}
		} catch (RuntimeException exception) {
			throw new DataInitializationException(exception);
		}
	}

	private List<ImportTrend> sampleImportTrends() {
		return List.of(
			new ImportTrend("3924", "플라스틱제 식탁용품", "2025-05", new BigDecimal("1000"), new BigDecimal("200")),
			new ImportTrend("8504", "전기 변압기", "2025-05", new BigDecimal("1000"), new BigDecimal("100")),
			new ImportTrend("9503", "완구류", "2025-05", new BigDecimal("1000"), new BigDecimal("150")),
			new ImportTrend("8516", "전열기기", "2025-05", new BigDecimal("800"), new BigDecimal("80")),
			new ImportTrend("9405", "조명기구", "2025-05", new BigDecimal("700"), new BigDecimal("70")),
			new ImportTrend("3924", "플라스틱제 식탁용품", "2026-05", new BigDecimal("2000"), new BigDecimal("400")),
			new ImportTrend("8504", "전기 변압기", "2026-05", new BigDecimal("1500"), new BigDecimal("150")),
			new ImportTrend("9503", "완구류", "2026-05", new BigDecimal("1200"), new BigDecimal("180")),
			new ImportTrend("8516", "전열기기", "2026-05", new BigDecimal("1180"), new BigDecimal("130")),
			new ImportTrend("9405", "조명기구", "2026-05", new BigDecimal("920"), new BigDecimal("96"))
		);
	}

	private List<EssentialIndustryItem> sampleEssentialItems() {
		return List.of(
			new EssentialIndustryItem("3924", "플라스틱제 식탁용품", "생활소비재", "수도권"),
			new EssentialIndustryItem("8504", "전기 변압기", "전기전자", "충청권"),
			new EssentialIndustryItem("8516", "전열기기", "전기전자", "영남권")
		);
	}
}
