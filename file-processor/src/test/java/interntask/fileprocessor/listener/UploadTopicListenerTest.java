package interntask.fileprocessor.listener;

import interntask.fileprocessor.service.ExcelValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UploadTopicListenerTest {

    private ExcelValidationService validationService;
    private KafkaTemplate<String, String> statusKafkaTemplate;
    private UploadTopicListener listener;

    @BeforeEach
    void setUp() {
        validationService = mock(ExcelValidationService.class);
        statusKafkaTemplate = mock(KafkaTemplate.class);
        listener = new UploadTopicListener(validationService, statusKafkaTemplate);
        // Установим topic через рефлексию или сеттер (лучше — сеттер)
        org.springframework.test.util.ReflectionTestUtils.setField(listener, "statusTopic", "status-topic");
    }

    @Test
    void listen_validFile_sendsProcessedStatus() {
        String fileHash = "abc123";
        byte[] content = new byte[0];

        when(validationService.validateFirstSheet(content)).thenReturn(true);

        listener.listen(content, fileHash);

        verify(statusKafkaTemplate).send(eq("status-topic"), eq(fileHash), anyString());

    }

    @Test
    void listen_invalidFile_sendsValidationFailedStatus() {
        String fileHash = "def456";
        byte[] content = new byte[0];

        when(validationService.validateFirstSheet(content)).thenReturn(false);

        listener.listen(content, fileHash);

        verify(statusKafkaTemplate).send(eq("status-topic"), eq(fileHash), anyString());
    }

    @Test
    void listen_exceptionDuringValidation_sendsProcessingError() {
        String fileHash = "error789";
        byte[] content = new byte[0];

        when(validationService.validateFirstSheet(content)).thenThrow(new RuntimeException("Boom!"));

        listener.listen(content, fileHash);

        verify(statusKafkaTemplate).send(eq("status-topic"), eq(fileHash), anyString());
    }
}