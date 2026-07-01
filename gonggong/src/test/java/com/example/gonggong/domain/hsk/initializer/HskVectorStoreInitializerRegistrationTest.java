package com.example.gonggong.domain.hsk.initializer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class HskVectorStoreInitializerRegistrationTest {

	@Test
	void initializerIsNotSkippedByVectorStoreBeanCondition() {
		assertThat(HskVectorStoreInitializer.class.getAnnotation(ConditionalOnBean.class)).isNull();
	}

	@Test
	void initializerDoesNotWriteInsideReadOnlyTransaction() throws NoSuchMethodException {
		Transactional transactional = HskVectorStoreInitializer.class
			.getMethod("run", String[].class)
			.getAnnotation(Transactional.class);

		assertThat(transactional == null || !transactional.readOnly()).isTrue();
	}
}
