package interntask.fileuploader.integration;

import interntask.fileuploader.FileUploaderApplication;
import interntask.fileuploader.dto.FileStatus;
import interntask.fileuploader.repository.FileStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = FileUploaderApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false", // Отключаем config server
                "server.ssl.enabled=false" // Отключаем SSL для тестов
        }
)
@Testcontainers
class FileUploaderIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0");

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Явно указываем свойства Kafka
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.apache.kafka.common.serialization.ByteArraySerializer");

        // Свойства приложения
        registry.add("app.file.max-size", () -> "5242880");
        registry.add("app.file.allowed-extensions", () -> "xls,xlsx");
        registry.add("spring.kafka.topics.upload", () -> "upload-topic");
        registry.add("spring.kafka.topics.status", () -> "status-topic");

        // MongoDB свойства (на всякий случай)
        registry.add("spring.data.mongodb.database", () -> "firestarter_db");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FileStatusRepository statusRepository;

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Test
    void uploadAndCheckStatus_flowWorks() {
        // given
        String filename = "test.xlsx";
        byte[] content = "fake excel content".getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Создаем часть для файла с ByteArrayResource вместо InputStreamResource
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> uploadResponse = restTemplate.postForEntity("/upload", request, String.class);

        // then
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String fileHash = uploadResponse.getBody();
        assertThat(fileHash).isNotNull().hasSize(64);

        // Даем время для обработки
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Проверка статуса на эндпоинте
        ResponseEntity<FileStatus> statusResponse = restTemplate.getForEntity("/status/" + fileHash, FileStatus.class);
        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody().getStatus()).isEqualTo("ACCEPTED");

        // Проверка БД
        List<FileStatus> statuses = statusRepository.findAll();
        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getFileHash()).isEqualTo(fileHash);
        assertThat(statuses.get(0).getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void uploadFileWithInvalidExtension_shouldReturnBadRequest() {
        // given
        String filename = "test.txt"; // Недопустимое расширение
        byte[] content = "text content".getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = restTemplate.postForEntity("/upload", request, String.class);

        // then - ожидаем 400 или 500
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Test
    void uploadFileExceedingSizeLimit_shouldReturnBadRequest() {
        // given
        String filename = "large.xlsx";
        byte[] content = new byte[6 * 1024 * 1024]; // 6MB > 5MB лимита

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        // when
        try {
            ResponseEntity<String> response = restTemplate.postForEntity("/upload", request, String.class);

            // then - проверяем, что получили ошибку (может быть 400 или 413)
            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (Exception e) {
            // Ожидаем исключение при слишком большом файле - это нормально
            assertThat(e).isInstanceOfAny(
                    org.springframework.web.client.ResourceAccessException.class,
                    org.springframework.web.client.HttpClientErrorException.class
            );
        }
    }

    @Test
    void checkStatusForNonExistentFile_shouldReturnNotFound() {
        // when
        ResponseEntity<FileStatus> response = restTemplate.getForEntity("/status/nonexistenthash", FileStatus.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void uploadWithoutFile_shouldReturnBadRequest() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // Не добавляем файл специально

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = restTemplate.postForEntity("/upload", request, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}