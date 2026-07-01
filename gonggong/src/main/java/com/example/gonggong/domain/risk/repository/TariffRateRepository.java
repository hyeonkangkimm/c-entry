package com.example.gonggong.domain.risk.repository;

import com.example.gonggong.domain.risk.domain.TariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {

	@Query("""
		select rate
		from TariffRate rate
		where rate.hskCode = :hskCode
		  and rate.active = true
		  and (rate.effectiveFrom is null or rate.effectiveFrom <= :date)
		  and (rate.effectiveTo is null or rate.effectiveTo >= :date)
		order by rate.effectiveFrom desc
		""")
	List<TariffRate> findEffectiveRates(
		@Param("hskCode") String hskCode,
		@Param("date") LocalDate date
	);
}
