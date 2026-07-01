package com.example.gonggong.domain.hsk.initializer;

import com.example.gonggong.domain.hsk.dataset.HskDatasetReader;
import com.example.gonggong.domain.hsk.dataset.HskDatasetRow;
import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.repository.HskItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class HskDatasetInitializerTest {

	@Test
	void reloadsDatasetWhenExistingCountDiffersFromCurrentFile() {
		HskItemRepository repository = mock(HskItemRepository.class);
		HskDatasetReader reader = mock(HskDatasetReader.class);
		Resource resource = new ByteArrayResource(new byte[0]);
		when(repository.count()).thenReturn(500L);
		when(reader.read(resource)).thenReturn(List.of(
			new HskDatasetRow("0101211000", "말", "Horses", "말"),
			new HskDatasetRow("3924100000", "플라스틱 식탁용품", "Tableware and kitchenware, of plastics", "식탁용품 > 플라스틱 식탁용품")
		));

		HskDatasetInitializer initializer = new HskDatasetInitializer(repository, reader, resource, true);
		initializer.run();

		verify(repository).deleteAllInBatch();
		verify(repository, times(1)).saveAll(anyList());
		verify(repository, atLeastOnce()).flush();
	}

	@Test
	void skipsReloadWhenCountsAlreadyMatch() {
		HskItemRepository repository = mock(HskItemRepository.class);
		HskDatasetReader reader = mock(HskDatasetReader.class);
		Resource resource = new ByteArrayResource(new byte[0]);
		when(repository.count()).thenReturn(2L);
		when(repository.countByDisplayNameIsNull()).thenReturn(0L);
		when(reader.read(resource)).thenReturn(List.of(
			new HskDatasetRow("0101211000", "말", "Horses", "말"),
			new HskDatasetRow("3924100000", "플라스틱 식탁용품", "Tableware and kitchenware, of plastics", "식탁용품 > 플라스틱 식탁용품")
		));

		HskDatasetInitializer initializer = new HskDatasetInitializer(repository, reader, resource, true);
		initializer.run();

		verify(repository, never()).deleteAllInBatch();
		verify(repository, never()).saveAll(anyList());
	}

	@Test
	void reloadsDatasetWhenDisplayNamesAreMissing() {
		HskItemRepository repository = mock(HskItemRepository.class);
		HskDatasetReader reader = mock(HskDatasetReader.class);
		Resource resource = new ByteArrayResource(new byte[0]);
		when(repository.count()).thenReturn(1L);
		when(repository.countByDisplayNameIsNull()).thenReturn(1L);
		when(reader.read(resource)).thenReturn(List.of(
			new HskDatasetRow("9102119090", "기타", "Other", "손목시계/휴대용 시계 > 전기구동식 > 기타")
		));

		HskDatasetInitializer initializer = new HskDatasetInitializer(repository, reader, resource, true);
		initializer.run();

		verify(repository).deleteAllInBatch();
		verify(repository).saveAll(anyList());
	}
}
