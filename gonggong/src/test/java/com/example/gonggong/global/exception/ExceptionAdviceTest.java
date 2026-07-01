package com.example.gonggong.global.exception;

import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExceptionAdviceTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new ThrowingController())
			.setControllerAdvice(new ExceptionAdvice())
			.build();
	}

	@Test
	void domainErrorCodeCanBeUsedAsBaseCode() {
		BaseCode errorCode = AnalysisErrorCode.INVALID_PRODUCT_NAME;

		assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(errorCode.getCode()).isEqualTo("ANALYSIS_001");
		assertThat(errorCode.getMessage()).isEqualTo("상품명은 필수입니다.");
	}

	@Test
	void handlesCustomExceptionWithBaseCodeResponse() throws Exception {
		mockMvc.perform(get("/throw/custom"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("ANALYSIS_001"))
			.andExpect(jsonPath("$.message").value("상품명은 필수입니다."));
	}

	@Test
	void handlesDataInitializationException() throws Exception {
		mockMvc.perform(get("/throw/data-initialization"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.code").value("GLOBAL_003"))
			.andExpect(jsonPath("$.message").value("초기 데이터 적재 중 오류가 발생했습니다."));
	}

	@Test
	void handlesUnexpectedException() throws Exception {
		mockMvc.perform(get("/throw/unexpected"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.code").value("GLOBAL_001"))
			.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
	}

	@Test
	void handlesValidationExceptionAsBadRequest() throws Exception {
		mockMvc.perform(post("/throw/invalid-request")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("GLOBAL_002"));
	}

	@RestController
	static class ThrowingController {

		@GetMapping("/throw/custom")
		void throwCustomException() {
			throw new CustomException(AnalysisErrorCode.INVALID_PRODUCT_NAME);
		}

		@GetMapping("/throw/data-initialization")
		void throwDataInitializationException() {
			throw new DataInitializationException();
		}

		@GetMapping("/throw/unexpected")
		void throwUnexpectedException() {
			throw new IllegalStateException("unexpected");
		}

		@PostMapping("/throw/invalid-request")
		void invalidRequest(@Valid @RequestBody ValidationRequest request) {
		}
	}

	record ValidationRequest(@NotBlank String name) {
	}
}
