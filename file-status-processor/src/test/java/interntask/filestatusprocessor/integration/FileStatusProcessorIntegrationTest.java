package interntask.filestatusprocessor.integration;

import interntask.filestatusprocessor.FileStatusProcessorApplication;
import interntask.filestatusprocessor.dto.FileStatus;
import interntask.filestatusprocessor.repository.FileStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        classes = FileStatusProcessorApplication.class,
        properties = {
                "spring.cloud.config.enabled=false"
        }
)
@Testcontainers
class FileStatusProcessorIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("kafka.topics.status", () -> "status-topic-it");
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private FileStatusRepository fileStatusRepository;

    @BeforeEach
    void cleanDb() {
        fileStatusRepository.deleteAll();
    }

    @AfterEach
    void cleanAfter() {
        fileStatusRepository.deleteAll();
    }

    @Test
    void processStatusMessage_updatesMongoDb() {
        // Given
        String fileHash = "integration-test-123";
        String jsonMessage = """
            {"status":"PROCESSED","message":"Secondary validation passed"}
            """;

        // When
        kafkaTemplate.send("status-topic-it", fileHash, jsonMessage);

        // Then: ждём, пока сообщение обработается и сохранится в MongoDB
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<FileStatus> statuses = fileStatusRepository.findAll();
            assertThat(statuses).hasSize(1);
            FileStatus status = statuses.get(0);
            assertThat(status.getFileHash()).isEqualTo(fileHash);
            assertThat(status.getStatus()).isEqualTo("PROCESSED");
            assertThat(status.getMessage()).isEqualTo("Secondary validation passed");
        });
    }

    @Test
    void processDuplicateMessage_isIdempotent() {
        String fileHash = "idempotent-test";
        String json1 = """
            {"status":"ACCEPTED","message":"Initial upload"}
            """;
        String json2 = """
            {"status":"ACCEPTED","message":"Initial upload"}  // same
            """;

        kafkaTemplate.send("status-topic-it", fileHash, json1);
        kafkaTemplate.send("status-topic-it", fileHash, json2);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<FileStatus> statuses = fileStatusRepository.findAll();
            assertThat(statuses).hasSize(1); // только одна запись
            FileStatus status = statuses.get(0);
            assertThat(status.getStatus()).isEqualTo("ACCEPTED");
        });
    }

    @Test
    void processStatusUpdate_overwritesPreviousStatus() {
        String fileHash = "status-update-test";
        String json1 = """
            {"status":"ACCEPTED","message":"Uploaded"}
            """;
        String json2 = """
            {"status":"PROCESSED","message":"Validated"}
            """;

        kafkaTemplate.send("status-topic-it", fileHash, json1);
        kafkaTemplate.send("status-topic-it", fileHash, json2);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<FileStatus> statuses = fileStatusRepository.findAll();
            assertThat(statuses).hasSize(1);
            assertThat(statuses.get(0).getStatus()).isEqualTo("PROCESSED");
        });
    }

    @Test
    void processInvalidJson_doesNotCrashAndDoesNotSave() throws InterruptedException {
        String fileHash = "invalid-json-test";
        String invalidJson = "{ not valid json }";

        kafkaTemplate.send("status-topic-it", fileHash, invalidJson);

        // Ждём немного, чтобы убедиться, что ничего не сохранилось
        Thread.sleep(2000);

        List<FileStatus> statuses = fileStatusRepository.findAll();
        assertThat(statuses).isEmpty();
    }
}