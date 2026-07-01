package com.example.gonggong.global.common;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BaseTimeEntityTest {

	@Test
	void baseTimeEntityUsesJpaAuditingAnnotations() {
		assertThat(BaseTimeEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();

		EntityListeners entityListeners = BaseTimeEntity.class.getAnnotation(EntityListeners.class);
		assertThat(entityListeners).isNotNull();
		assertThat(Arrays.asList(entityListeners.value())).contains(AuditingEntityListener.class);
	}

	@Test
	void baseTimeEntityHasCreatedAtAndUpdatedAtFields() throws Exception {
		Field createdAt = BaseTimeEntity.class.getDeclaredField("createdAt");
		Field updatedAt = BaseTimeEntity.class.getDeclaredField("updatedAt");

		assertThat(createdAt.getType()).isEqualTo(LocalDateTime.class);
		assertThat(updatedAt.getType()).isEqualTo(LocalDateTime.class);
		assertThat(createdAt.isAnnotationPresent(CreatedDate.class)).isTrue();
		assertThat(updatedAt.isAnnotationPresent(LastModifiedDate.class)).isTrue();
	}
}
