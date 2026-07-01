package com.example.gonggong.domain.demand.repository;

@FunctionalInterface
public interface EssentialIndustryItemReader {

	boolean existsByHskCode(String hskCode);
}
