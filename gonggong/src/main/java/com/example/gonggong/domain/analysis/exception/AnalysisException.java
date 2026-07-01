package com.example.gonggong.domain.analysis.exception;

import com.example.gonggong.global.exception.CustomException;

public class AnalysisException extends CustomException {

	public AnalysisException(AnalysisErrorCode analysisErrorCode) {
		super(analysisErrorCode);
	}

	public AnalysisException(AnalysisErrorCode analysisErrorCode, Throwable cause) {
		super(analysisErrorCode, cause);
	}
}
