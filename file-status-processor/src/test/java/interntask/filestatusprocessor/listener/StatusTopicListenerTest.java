package interntask.filestatusprocessor.listener;

import interntask.filestatusprocessor.service.FileStatusUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class StatusTopicListenerTest {

    private FileStatusUpdateService updateService;
    private StatusTopicListener listener;

    @BeforeEach
    void setUp() {
        updateService = mock(FileStatusUpdateService.class);
        listener = new StatusTopicListener(updateService);
    }

    @Test
    void listen_validJson_callsUpdateService() {
        String fileHash = "xyz789";
        String json = """
            {"status":"ACCEPTED","message":"File uploaded"}
            """;

        listener.listen(fileHash, json);

        verify(updateService).updateStatus(fileHash, "ACCEPTED", "File uploaded");
    }

    @Test
    void listen_missingStatus_usesUnknown() {
        String fileHash = "missing-status";
        String json = """
            {"message":"No status field"}
            """;

        listener.listen(fileHash, json);

        verify(updateService).updateStatus(fileHash, "UNKNOWN", "No status field");
    }

    @Test
    void listen_missingMessage_usesDefault() {
        String fileHash = "missing-msg";
        String json = """
            {"status":"DONE"}
            """;

        listener.listen(fileHash, json);

        verify(updateService).updateStatus(fileHash, "DONE", "No message");
    }

    @Test
    void listen_invalidJson_logsErrorAndDoesNotCallService() {
        String fileHash = "bad-json";
        String json = "{ invalid json }";

        listener.listen(fileHash, json);

        verify(updateService, never()).updateStatus(any(), any(), any());
        // Логирование не проверяем — оно происходит, но не ломает
    }

    @Test
    void listen_emptyJson_handlesGracefully() {
        listener.listen("empty", "{}");
        verify(updateService).updateStatus("empty", "UNKNOWN", "No message");
    }
}