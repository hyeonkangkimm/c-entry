package com.example.gonggong.domain.demand.dto;

import java.util.List;

public record DemandPriorityTop10Response(
	List<DemandPriorityItemResponse> items
) {
}
