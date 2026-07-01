package com.example.gonggong.domain.hsk.dto;

import java.util.List;

public record HskMatchResponse(
	boolean matched,
	List<HskCandidateResponse> candidates,
	String message
) {
}
