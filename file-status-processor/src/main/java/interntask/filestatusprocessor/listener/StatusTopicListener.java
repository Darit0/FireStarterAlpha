package interntask.filestatusprocessor.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import interntask.filestatusprocessor.service.FileStatusUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class StatusTopicListener {

    private static final Logger log = LoggerFactory.getLogger(StatusTopicListener.class);

    private final FileStatusUpdateService updateService;
    private final ObjectMapper objectMapper;

    public StatusTopicListener(FileStatusUpdateService updateService) {
        this.updateService = updateService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "${kafka.topics.status:status-topic}", groupId = "file-status-processor-group")
    public void listen(@Payload String jsonMessage,
                       @Header(KafkaHeaders.RECEIVED_KEY) String fileHash) {
        log.info("Received status update for file {}: {}", fileHash, jsonMessage);

        try {
            JsonNode json = objectMapper.readTree(jsonMessage);
            String status = json.has("status") ? json.get("status").asText() : "UNKNOWN";
            String message = json.has("message") ? json.get("message").asText() : "No message";

            updateService.updateStatus(fileHash, status, message);
        } catch (Exception e) {
            log.error("Error parsing or processing status message for file " + fileHash, e);
        }
    }
}