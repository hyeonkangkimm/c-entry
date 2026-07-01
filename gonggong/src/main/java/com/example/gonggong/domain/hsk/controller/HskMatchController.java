package com.example.gonggong.domain.hsk.controller;

import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.example.gonggong.domain.hsk.dto.HskMatchResponse;
import com.example.gonggong.domain.hsk.service.HskMatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/hsk")
public class HskMatchController {

	private final HskMatchService hskMatchService;

	public HskMatchController(HskMatchService hskMatchService) {
		this.hskMatchService = hskMatchService;
	}

	@PostMapping("/match")
	public HskMatchResponse match(@Valid @RequestBody HskMatchRequest request) {
		return hskMatchService.match(request);
	}
}
