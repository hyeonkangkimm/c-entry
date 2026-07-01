package com.example.gonggong.domain.demand.exception;

import com.example.gonggong.global.exception.CustomException;

public class DemandException extends CustomException {

	public DemandException(DemandErrorCode demandErrorCode) {
		super(demandErrorCode);
	}

	public DemandException(DemandErrorCode demandErrorCode, Throwable cause) {
		super(demandErrorCode, cause);
	}
}
