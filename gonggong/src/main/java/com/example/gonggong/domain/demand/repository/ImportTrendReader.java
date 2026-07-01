package com.example.gonggong.domain.demand.repository;

import com.example.gonggong.domain.demand.entity.ImportTrend;

import java.util.List;
import java.util.Optional;

public interface ImportTrendReader {

	Optional<String> findLatestYearMonth();

	List<ImportTrend> findByYearMonth(String yearMonth);

	Optional<ImportTrend> findByHskCodeAndYearMonth(String hskCode, String yearMonth);
}
