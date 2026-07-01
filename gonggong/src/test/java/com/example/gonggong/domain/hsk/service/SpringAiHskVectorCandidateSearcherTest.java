package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.example.gonggong.domain.hsk.repository.HskItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class SpringAiHskVectorCandidateSearcherTest {

	@Test
	void logsVectorSearchQueryAndTopHskCodes(CapturedOutput output) {
		VectorStore vectorStore = mock(VectorStore.class);
		HskItemRepository repository = mock(HskItemRepository.class);
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
			new Document("mouse", Map.of("hskCode", "8471601030")),
			new Document("keyboard", Map.of("hskCode", "8471601020"))
		));
		when(repository.findByHskCodeIn(List.of("8471601030", "8471601020"))).thenReturn(List.of(
			new HskItem("8471601030", "마우스", "Mouse"),
			new HskItem("8471601020", "키보드", "Keyboard")
		));
		SpringAiHskVectorCandidateSearcher searcher = new SpringAiHskVectorCandidateSearcher(
			new StaticObjectProvider<>(vectorStore),
			repository,
			new HskVectorQueryBuilder()
		);

		searcher.search(
			new HskMatchRequest("게이밍 마우스", "USB 유선", null),
			new HskFeatures("게이밍 마우스", List.of("마우스"), "컴퓨터용 마우스", "FINISHED_PRODUCT", List.of("마우스"), List.of(), List.of()),
			20
		);

		assertThat(output).contains("HSK vector search completed");
		assertThat(output).contains("resultCount=2");
		assertThat(output).contains("topHskCodes=[8471601030, 8471601020]");
	}

	private record StaticObjectProvider<T>(T value) implements ObjectProvider<T> {

		@Override
		public T getObject(Object... args) {
			return value;
		}

		@Override
		public T getIfAvailable() {
			return value;
		}

		@Override
		public T getObject() {
			return value;
		}
	}
}
