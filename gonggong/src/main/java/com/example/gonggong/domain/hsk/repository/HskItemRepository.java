package com.example.gonggong.domain.hsk.repository;

import com.example.gonggong.domain.hsk.domain.HskItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HskItemRepository extends JpaRepository<HskItem, Long> {

	@Query("""
		select item
		from HskItem item
		where lower(item.koreanName) like lower(concat('%', :keyword, '%'))
		   or lower(item.englishName) like lower(concat('%', :keyword, '%'))
		   or lower(item.displayName) like lower(concat('%', :keyword, '%'))
		order by item.hskCode
		""")
	List<HskItem> searchByName(@Param("keyword") String keyword, Pageable pageable);

	List<HskItem> findByHskCodeIn(List<String> hskCodes);

	long countByDisplayNameIsNull();
}
