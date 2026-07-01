package com.example.gonggong.domain.risk.repository;

import com.example.gonggong.domain.risk.domain.HskKcRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HskKcRequirementRepository extends JpaRepository<HskKcRequirement, Long> {

	boolean existsByHskCodePrefix(String hskCodePrefix);

	List<HskKcRequirement> findByActiveTrue();
}
