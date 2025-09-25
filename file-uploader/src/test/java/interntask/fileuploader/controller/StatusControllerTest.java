package interntask.fileuploader.controller;


import interntask.fileuploader.dto.FileStatus;
import interntask.fileuploader.repository.FileStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatusController.class)
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStatusRepository statusRepository;

    @Test
    void getFileStatus_existingHash_returnsStatus() throws Exception {
        String hash = "abc123";
        FileStatus status = new FileStatus(hash, "ACCEPTED", "OK");
        when(statusRepository.findByFileHash(hash)).thenReturn(Optional.of(status));

        mockMvc.perform(get("/status/" + hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void getFileStatus_nonExistingHash_returns404() throws Exception {
        when(statusRepository.findByFileHash("xyz")).thenReturn(Optional.empty());

        mockMvc.perform(get("/status/xyz"))
                .andExpect(status().isNotFound());
    }
}