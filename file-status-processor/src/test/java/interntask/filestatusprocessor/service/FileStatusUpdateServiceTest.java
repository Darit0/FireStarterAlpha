package interntask.filestatusprocessor.service;

import interntask.filestatusprocessor.repository.FileStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;


import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FileStatusUpdateServiceTest {

    private FileStatusRepository repository;
    private FileStatusUpdateService service;

    @BeforeEach
    void setUp() {
        repository = mock(FileStatusRepository.class);
        service = new FileStatusUpdateService(repository);
    }

    @Test
    void updateStatus_validData_savesToRepository() {
        String fileHash = "abc123";
        String status = "PROCESSED";
        String message = "OK";

        service.updateStatus(fileHash, status, message);

        verify(repository).save(argThat(fs ->
                fs.getFileHash().equals(fileHash) &&
                        fs.getStatus().equals(status) &&
                        fs.getMessage().equals(message)
        ));
    }

    @Test
    void updateStatus_repositoryThrowsException_propagatesException() {
        doThrow(new DataAccessException("DB down") {}).when(repository).save(any());

        assertThatThrownBy(() -> service.updateStatus("hash", "STATUS", "msg"))
                .isInstanceOf(DataAccessException.class);
    }
}