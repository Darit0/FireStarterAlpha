package interntask.fileuploader.service;


import interntask.fileuploader.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidationServiceTest {

    private FileValidationService service;

    @BeforeEach
    void setUp() {
        service = new FileValidationService();

        long maxSizeBytes = 5 * 1024 * 1024; // 5 MB в байтах
        org.springframework.test.util.ReflectionTestUtils.setField(service, "maxSizeMb", maxSizeBytes);

        org.springframework.test.util.ReflectionTestUtils.setField(service, "allowedExtensionsStr", "xls, xlsx");
    }

    @Test
    void validateFile_tooLarge_throwsException() {
        // 6 MB в байтах (превышает лимит 5 MB)
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", largeContent);

        assertThatThrownBy(() -> service.validateFile(file))
                .isInstanceOf(FileValidationException.class)
                .hasFieldOrPropertyWithValue("errorType", "FILE_TOO_LARGE");
    }

    @Test
    void validateFile_invalidExtension_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> service.validateFile(file))
                .isInstanceOf(FileValidationException.class)
                .hasFieldOrPropertyWithValue("errorType", "UNSUPPORTED_EXTENSION");
    }

    @Test
    void validateFile_validXlsxFile_passes() {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes());

        assertThatCode(() -> service.validateFile(file)).doesNotThrowAnyException();
    }

    @Test
    void validateFile_validXlsFile_passes() {
        MockMultipartFile file = new MockMultipartFile("file", "test.xls",
                "application/vnd.ms-excel", "content".getBytes());

        assertThatCode(() -> service.validateFile(file)).doesNotThrowAnyException();
    }

    @Test
    void validateFile_noExtension_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test", "application/octet-stream", "content".getBytes());

        assertThatThrownBy(() -> service.validateFile(file))
                .isInstanceOf(FileValidationException.class)
                .hasFieldOrPropertyWithValue("errorType", "NO_EXTENSION");
    }
}