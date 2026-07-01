package com.example.gonggong.domain.risk.dataset;

import com.example.gonggong.domain.risk.domain.TariffType;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class TariffDatasetReader {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
	private static final String LATEST_SHEET = "2.12";
	private static final String SOURCE_NOTICE =
		"관세청_품목번호별 관세율표_20260211 (공공데이터포털 15051179)";

	public int read(Resource resource, int batchSize, Consumer<List<TariffDatasetRow>> batchConsumer) {
		Path temporaryFile = null;
		try {
			temporaryFile = Files.createTempFile("customs-tariff-rates-", ".xlsx");
			try (InputStream input = resource.getInputStream()) {
				Files.copy(input, temporaryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
			return readLatestSheet(temporaryFile, batchSize, batchConsumer);
		} catch (Exception exception) {
			throw new IllegalStateException("관세청 관세율 기준 데이터 파일을 읽지 못했습니다.", exception);
		} finally {
			deleteQuietly(temporaryFile);
		}
	}

	private int readLatestSheet(
		Path file,
		int batchSize,
		Consumer<List<TariffDatasetRow>> batchConsumer
	) throws Exception {
		try (OPCPackage pkg = OPCPackage.open(file.toFile())) {
			XSSFReader reader = new XSSFReader(pkg);
			StylesTable styles = reader.getStylesTable();
			SharedStrings strings = reader.getSharedStringsTable();
			XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
			while (sheets.hasNext()) {
				try (InputStream sheet = sheets.next()) {
					if (!LATEST_SHEET.equals(sheets.getSheetName())) {
						continue;
					}
					TariffSheetHandler handler = new TariffSheetHandler(batchSize, batchConsumer);
					XMLReader parser = XMLHelper.newXMLReader();
					parser.setContentHandler(new XSSFSheetXMLHandler(
						styles,
						null,
						strings,
						handler,
						new DataFormatter(),
						false
					));
					parser.parse(new InputSource(sheet));
					handler.flush();
					return handler.rowCount();
				}
			}
		}
		throw new IllegalStateException("최신 관세율 시트를 찾지 못했습니다: " + LATEST_SHEET);
	}

	private void deleteQuietly(Path file) {
		if (file == null) {
			return;
		}
		try {
			Files.deleteIfExists(file);
		} catch (IOException ignored) {
			// Temporary source cleanup must not hide the dataset parsing result.
		}
	}

	private static class TariffSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

		private final int batchSize;
		private final Consumer<List<TariffDatasetRow>> batchConsumer;
		private final List<TariffDatasetRow> batch;
		private final Map<Integer, String> cells = new HashMap<>();
		private int rowCount;

		private TariffSheetHandler(
			int batchSize,
			Consumer<List<TariffDatasetRow>> batchConsumer
		) {
			this.batchSize = batchSize;
			this.batchConsumer = batchConsumer;
			this.batch = new ArrayList<>(batchSize);
		}

		@Override
		public void startRow(int rowNum) {
			cells.clear();
		}

		@Override
		public void endRow(int rowNum) {
			if (rowNum == 0) {
				return;
			}
			String hskCode = value(0);
			if (!hskCode.matches("\\d{10}")) {
				return;
			}

			String tariffCode = value(1);
			batch.add(new TariffDatasetRow(
				hskCode,
				tariffCode,
				toTariffType(tariffCode),
				parseDecimal(value(2)),
				blankToNull(value(3)),
				blankToNull(value(4)),
				blankToNull(value(5)),
				blankToNull(value(6)),
				parseDate(value(7)),
				parseDate(value(8)),
				SOURCE_NOTICE + ", 관세율구분=" + tariffCode
			));
			rowCount++;
			if (batch.size() >= batchSize) {
				flush();
			}
		}

		@Override
		public void cell(String cellReference, String formattedValue, XSSFComment comment) {
			cells.put(columnIndex(cellReference), formattedValue == null ? "" : formattedValue.trim());
		}

		private void flush() {
			if (batch.isEmpty()) {
				return;
			}
			batchConsumer.accept(List.copyOf(batch));
			batch.clear();
		}

		private int rowCount() {
			return rowCount;
		}

		private String value(int column) {
			return cells.getOrDefault(column, "");
		}

		private int columnIndex(String cellReference) {
			int index = 0;
			for (int offset = 0; offset < cellReference.length(); offset++) {
				char character = cellReference.charAt(offset);
				if (!Character.isLetter(character)) {
					break;
				}
				index = index * 26 + (Character.toUpperCase(character) - 'A' + 1);
			}
			return index - 1;
		}

		private TariffType toTariffType(String tariffCode) {
			if ("A".equals(tariffCode) || "A1".equals(tariffCode)) {
				return TariffType.BASIC;
			}
			if (tariffCode.startsWith("C")) {
				return TariffType.WTO;
			}
			if (tariffCode.startsWith("F")) {
				return TariffType.FTA;
			}
			return TariffType.UNKNOWN;
		}

		private BigDecimal parseDecimal(String value) {
			if (value == null || value.isBlank()) {
				return null;
			}
			try {
				return new BigDecimal(value);
			} catch (NumberFormatException exception) {
				return null;
			}
		}

		private LocalDate parseDate(String value) {
			if (value == null || value.isBlank()) {
				return null;
			}
			try {
				return LocalDate.parse(value, DATE_FORMAT);
			} catch (DateTimeParseException exception) {
				return null;
			}
		}

		private String blankToNull(String value) {
			return value == null || value.isBlank() ? null : value;
		}
	}
}
