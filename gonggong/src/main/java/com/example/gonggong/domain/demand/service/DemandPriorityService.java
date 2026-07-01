package com.example.gonggong.domain.demand.service;

import com.example.gonggong.domain.demand.dto.DemandPriorityItemResponse;
import com.example.gonggong.domain.demand.dto.DemandPriorityTop10Response;
import com.example.gonggong.domain.demand.entity.ImportTrend;
import com.example.gonggong.domain.demand.exception.DemandErrorCode;
import com.example.gonggong.domain.demand.exception.DemandException;
import com.example.gonggong.domain.demand.repository.EssentialIndustryItemReader;
import com.example.gonggong.domain.demand.repository.ImportTrendReader;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
public class DemandPriorityService {

	private static final int MAX_IMPORT_AMOUNT_SCORE = 35;
	private static final int MAX_IMPORT_WEIGHT_SCORE = 35;
	private static final int ESSENTIAL_PART_SCORE = 15;

	private final ImportTrendReader importTrendReader;
	private final EssentialIndustryItemReader essentialIndustryItemReader;

	public DemandPriorityService(
		ImportTrendReader importTrendReader,
		EssentialIndustryItemReader essentialIndustryItemReader
	) {
		this.importTrendReader = importTrendReader;
		this.essentialIndustryItemReader = essentialIndustryItemReader;
	}

	public DemandPriorityTop10Response getTop10PriorityItems() {
		String latestYearMonth = importTrendReader.findLatestYearMonth()
			.orElseThrow(() -> new DemandException(DemandErrorCode.IMPORT_TREND_DATA_EMPTY));
		String previousYearMonth = YearMonth.parse(latestYearMonth).minusYears(1).toString();

		List<DemandPriorityItemResponse> items = importTrendReader.findByYearMonth(latestYearMonth)
			.stream()
			.map(current -> toPriorityItem(current, previousYearMonth))
			.sorted(Comparator.comparingInt(DemandPriorityItemResponse::priorityScore).reversed())
			.limit(10)
			.toList();

		return new DemandPriorityTop10Response(items);
	}

	private DemandPriorityItemResponse toPriorityItem(ImportTrend current, String previousYearMonth) {
		ImportTrend previous = importTrendReader
			.findByHskCodeAndYearMonth(current.getHskCode(), previousYearMonth)
			.orElse(null);
		BigDecimal amountGrowthRate = growthRate(current.getImportAmount(), previous == null ? BigDecimal.ZERO : previous.getImportAmount());
		BigDecimal weightGrowthRate = growthRate(current.getImportWeight(), previous == null ? BigDecimal.ZERO : previous.getImportWeight());
		boolean essentialPart = essentialIndustryItemReader.existsByHskCode(current.getHskCode());
		int recallCount = 0;
		int priorityScore = calculatePriorityScore(amountGrowthRate, weightGrowthRate, essentialPart, recallCount);

		return new DemandPriorityItemResponse(
			current.getHskCode(),
			current.getItemName(),
			amountGrowthRate,
			weightGrowthRate,
			recallCount,
			essentialPart,
			priorityScore,
			priorityLevel(priorityScore),
			reason(amountGrowthRate, weightGrowthRate, essentialPart, recallCount)
		);
	}

	private BigDecimal growthRate(BigDecimal current, BigDecimal previous) {
		if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
			return current.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100.00") : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		return current.subtract(previous)
			.multiply(new BigDecimal("100"))
			.divide(previous, 2, RoundingMode.HALF_UP);
	}

	private int calculatePriorityScore(
		BigDecimal amountGrowthRate,
		BigDecimal weightGrowthRate,
		boolean essentialPart,
		int recallCount
	) {
		int amountScore = growthScore(amountGrowthRate, MAX_IMPORT_AMOUNT_SCORE);
		int weightScore = growthScore(weightGrowthRate, MAX_IMPORT_WEIGHT_SCORE);
		int essentialScore = essentialPart ? ESSENTIAL_PART_SCORE : 0;
		int recallScore = Math.min(recallCount * 3, 15);

		return Math.min(amountScore + weightScore + essentialScore + recallScore, 100);
	}

	private int growthScore(BigDecimal growthRate, int maxScore) {
		BigDecimal cappedRate = growthRate.max(BigDecimal.ZERO).min(new BigDecimal("100"));
		return cappedRate
			.multiply(BigDecimal.valueOf(maxScore))
			.divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP)
			.intValue();
	}

	private String priorityLevel(int priorityScore) {
		if (priorityScore >= 80) {
			return "HIGH";
		}
		if (priorityScore >= 50) {
			return "MEDIUM";
		}
		return "LOW";
	}

	private String reason(
		BigDecimal amountGrowthRate,
		BigDecimal weightGrowthRate,
		boolean essentialPart,
		int recallCount
	) {
		StringBuilder builder = new StringBuilder()
			.append("수입액 증가율 ")
			.append(amountGrowthRate)
			.append("%, 수입중량 증가율 ")
			.append(weightGrowthRate)
			.append("%");

		if (essentialPart) {
			builder.append(", 필수 산업 품목");
		}
		if (recallCount > 0) {
			builder.append(", 과거 리콜 빈도 ").append(recallCount).append("건");
		}

		return builder.toString();
	}
}
