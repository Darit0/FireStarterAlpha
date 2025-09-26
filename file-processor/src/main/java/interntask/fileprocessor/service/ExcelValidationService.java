package interntask.fileprocessor.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ExcelValidationService {

    public boolean validateFirstSheet(byte[] fileContent) {
        try (InputStream is = new ByteArrayInputStream(fileContent)) {
            Workbook workbook = detectWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            // Проверяем первые 2 строки
            for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) return false;

                // Проверяем первые 3 колонки (A, B, C → индексы 0,1,2)
                for (int cellIndex = 0; cellIndex < 3; cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    if (cell == null || isCellEmpty(cell)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Workbook detectWorkbook(InputStream is) throws IOException {
        //Проверка сигнатуры
        byte[] header = new byte[8];
        is.mark(8);
        is.read(header);
        is.reset();

        if (header[0] == (byte) 0xD0 && header[1] == (byte) 0xCF) {
            return new HSSFWorkbook(is); // .xls
        } else {
            return new XSSFWorkbook(is); // .xlsx
        }
    }

    private boolean isCellEmpty(Cell cell) {
        if (cell == null) return true;
        switch (cell.getCellType()) {
            case BLANK:
                return true;
            case STRING:
                return cell.getStringCellValue().trim().isEmpty();
            case NUMERIC:
                // Число — не пустое
                return false;
            case BOOLEAN:
                return false;
            default:
                return true;
        }
    }
}
