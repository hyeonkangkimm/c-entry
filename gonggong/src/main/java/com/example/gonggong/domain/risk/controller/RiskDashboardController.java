package com.example.gonggong.domain.risk.controller;

import com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest;
import com.example.gonggong.domain.risk.dto.response.RiskDashboardResponse;
import com.example.gonggong.domain.risk.service.RiskDashboardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk-dashboard")
public class RiskDashboardController {

	private final RiskDashboardService riskDashboardService;

	public RiskDashboardController(RiskDashboardService riskDashboardService) {
		this.riskDashboardService = riskDashboardService;
	}

	@PostMapping("/analyze")
	public RiskDashboardResponse analyze(@Valid @RequestBody RiskDashboardAnalyzeRequest request) {
		return riskDashboardService.analyze(request);
	}
}
