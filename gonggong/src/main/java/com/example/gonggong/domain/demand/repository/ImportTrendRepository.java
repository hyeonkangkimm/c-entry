package com.example.gonggong.domain.demand.repository;

import com.example.gonggong.domain.demand.entity.ImportTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ImportTrendRepository extends JpaRepository<ImportTrend, Long>, ImportTrendReader {

	@Override
	@Query("select max(importTrend.yearMonth) from ImportTrend importTrend")
	Optional<String> findLatestYearMonth();
}
