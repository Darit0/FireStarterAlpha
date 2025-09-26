package interntask.fileprocessor.listener;

import interntask.fileprocessor.dto.FileStatusUpdate;
import interntask.fileprocessor.service.ExcelValidationService;
import interntask.fileprocessor.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class UploadTopicListener {

    private static final Logger log = LoggerFactory.getLogger(UploadTopicListener.class);

    private final ExcelValidationService validationService;
    private final KafkaTemplate<String, String> statusKafkaTemplate;

    @Value("${kafka.topics.status:status-topic}")
    private String statusTopic;

    public UploadTopicListener(ExcelValidationService validationService,
                               KafkaTemplate<String, String> statusKafkaTemplate) {
        this.validationService = validationService;
        this.statusKafkaTemplate = statusKafkaTemplate;
    }

    @KafkaListener(topics = "${kafka.topics.upload:upload-topic}", groupId = "file-processor-group")
    public void listen(@Payload byte[] fileContent,
                       @Header(KafkaHeaders.RECEIVED_KEY) String fileHash) {
        log.info("Processing file with hash: {}", fileHash);

        try {
            boolean valid = validationService.validateFirstSheet(fileContent);
            String status = valid ? "PROCESSED" : "SECONDARY_VALIDATION_FAILED";
            String message = valid ? "File passed secondary validation" : "First sheet missing data in A1:C2";

            FileStatusUpdate update = new FileStatusUpdate(fileHash, status, message);
            String json = update.toJson().replace("%s", JsonUtil.escapeJson(fileHash))
                    .replace("%s", status)
                    .replace("%s", JsonUtil.escapeJson(message));

            statusKafkaTemplate.send(statusTopic, fileHash, json);
            log.info("Sent status update for {}: {}", fileHash, status);

        } catch (Exception e) {
            log.error("Error processing file {}", fileHash, e);
            FileStatusUpdate errorUpdate = new FileStatusUpdate(
                    fileHash, "PROCESSING_ERROR", "Failed to validate file: " + e.getMessage()
            );
            statusKafkaTemplate.send(statusTopic, fileHash, errorUpdate.toJson());
        }
    }
}
