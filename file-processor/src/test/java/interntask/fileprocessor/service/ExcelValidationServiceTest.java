package interntask.fileprocessor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelValidationServiceTest {

    private final ExcelValidationService service = new ExcelValidationService();

    @Test
    void validateFirstSheet_validXlsx_returnsTrue() throws IOException {
        byte[] content = createValidXlsx();
        assertThat(service.validateFirstSheet(content)).isTrue();
    }

    @Test
    void validateFirstSheet_validXls_returnsTrue() throws IOException {
        byte[] content = createValidXls();
        assertThat(service.validateFirstSheet(content)).isTrue();
    }

    @Test
    void validateFirstSheet_missingCellInA1_returnsFalse() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row0 = sheet.createRow(0);// A1 пустой
            row0.createCell(1).setCellValue("B1");
            row0.createCell(2).setCellValue("C1");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("A2");
            row1.createCell(1).setCellValue("B2");
            row1.createCell(2).setCellValue("C2");

            byte[] content = toByteArray(workbook);
            assertThat(service.validateFirstSheet(content)).isFalse();
        }
    }

    @Test
    void validateFirstSheet_emptyStringInB2_returnsFalse() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("A1");
            row0.createCell(1).setCellValue("B1");
            row0.createCell(2).setCellValue("C1");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("A2");
            row1.createCell(1).setCellValue(""); // пустая строка
            row1.createCell(2).setCellValue("C2");

            byte[] content = toByteArray(workbook);
            assertThat(service.validateFirstSheet(content)).isFalse();
        }
    }

    @Test
    void validateFirstSheet_numericValues_valid() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue(1.0);
            row0.createCell(1).setCellValue(2.0);
            row0.createCell(2).setCellValue(3.0);
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(4.0);
            row1.createCell(1).setCellValue(5.0);
            row1.createCell(2).setCellValue(6.0);

            byte[] content = toByteArray(workbook);
            assertThat(service.validateFirstSheet(content)).isTrue();
        }
    }

    @Test
    void validateFirstSheet_corruptedFile_returnsFalse() {
        byte[] garbage = "not an excel file".getBytes();
        assertThat(service.validateFirstSheet(garbage)).isFalse();
    }

    //Вспомогательные методы для имитации файлов

    private byte[] createValidXlsx() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            for (int r = 0; r < 2; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < 3; c++) {
                    row.createCell(c).setCellValue("R" + r + "C" + c);
                }
            }
            return toByteArray(workbook);
        }
    }

    private byte[] createValidXls() throws IOException {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            for (int r = 0; r < 2; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < 3; c++) {
                    row.createCell(c).setCellValue("R" + r + "C" + c);
                }
            }
            return toByteArray(workbook);
        }
    }

    private byte[] toByteArray(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }
}