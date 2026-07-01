package com.example.gonggong.domain.demand.controller;

import com.example.gonggong.domain.demand.dto.DemandPriorityTop10Response;
import com.example.gonggong.domain.demand.service.DemandPriorityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demand")
public class DemandPriorityController {

	private final DemandPriorityService demandPriorityService;

	public DemandPriorityController(DemandPriorityService demandPriorityService) {
		this.demandPriorityService = demandPriorityService;
	}

	@GetMapping("/priority-items/top10")
	public DemandPriorityTop10Response getTop10PriorityItems() {
		return demandPriorityService.getTop10PriorityItems();
	}
}
