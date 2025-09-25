package interntask.fileuploader.controller;

import interntask.fileuploader.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UploadController.class)
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploadService uploadService;

    @Test
    void uploadFile_validFile_returnsHash() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes());
        when(uploadService.handleFileUpload(file)).thenReturn("abc123");

        mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("abc123"));
    }

    @Test
    void uploadFile_invalidFile_returnsErrorJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "bad".getBytes());
        when(uploadService.handleFileUpload(file))
                .thenThrow(new RuntimeException("TEST"));

        mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.errorType").exists());
    }
}