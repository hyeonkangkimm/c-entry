package com.example.gonggong.domain.demand.repository;

import com.example.gonggong.domain.demand.entity.EssentialIndustryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialIndustryItemRepository extends JpaRepository<EssentialIndustryItem, Long>, EssentialIndustryItemReader {
}
