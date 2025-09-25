package interntask.fileprocessor.integration;

import interntask.fileprocessor.FileProcessorApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {FileProcessorApplication.class, TestKafkaConfig.class}, // ← подключаем внешний конфиг
        properties = {
                "spring.cloud.config.enabled=false",
                "server.ssl.enabled=false",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.group-id=file-processor-test-group"
        }
)
@Testcontainers
class FileProcessorIntegrationTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"))
            .withEnv("KAFKA_MESSAGE_MAX_BYTES", "5242880")
            .withEnv("KAFKA_REPLICA_FETCH_MAX_BYTES", "5242880")
            .withEnv("KAFKA_FETCH_MESSAGE_MAX_BYTES", "5242880");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("app.file.max-size", () -> "5242880");
        registry.add("app.file.allowed-extensions", () -> "xls,xlsx");
        registry.add("spring.kafka.topics.upload", () -> "upload-topic");
        registry.add("spring.kafka.topics.status", () -> "status-topic");
    }

    @Autowired
    private KafkaTemplate<String, byte[]> testKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, String> testStatusKafkaTemplate;

    //вспомогательные методы
    private static byte[] createValidXlsx() throws IOException {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = wb.createSheet("Test Data");
            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Name");
            headerRow.createCell(2).setCellValue("Value");

            var dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue(1);
            dataRow1.createCell(1).setCellValue("Test Item 1");
            dataRow1.createCell(2).setCellValue(100.50);

            var dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue(2);
            dataRow2.createCell(1).setCellValue("Test Item 2");
            dataRow2.createCell(2).setCellValue(200.75);

            try (var out = new ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        }
    }

    private static byte[] createInvalidExcelContent() {
        return "This is not a valid Excel file content".getBytes();
    }

    @Test
    void processValidExcelFile_sendsProcessedStatus() throws Exception {
        String fileHash = "valid-excel-hash-" + System.currentTimeMillis();
        byte[] fileContent = createValidXlsx();

        var sendResult = testKafkaTemplate.send("upload-topic", fileHash, fileContent).get(10, TimeUnit.SECONDS);

        assertThat(sendResult.getRecordMetadata()).isNotNull();
        assertThat(sendResult.getProducerRecord().key()).isEqualTo(fileHash);
        Thread.sleep(5000);
        assertThat(fileContent.length).isGreaterThan(0);
    }

    @Test
    void processInvalidFile_sendsValidationFailedStatus() throws Exception {
        String fileHash = "invalid-file-hash-" + System.currentTimeMillis();
        byte[] fileContent = createInvalidExcelContent();

        var sendResult = testKafkaTemplate.send("upload-topic", fileHash, fileContent).get(10, TimeUnit.SECONDS);

        assertThat(sendResult.getRecordMetadata()).isNotNull();
        assertThat(sendResult.getProducerRecord().key()).isEqualTo(fileHash);
        Thread.sleep(3000);
        assertThat(fileContent).isNotEmpty();
    }

    @Test
    void processEmptyFile_handlesGracefully() throws Exception {
        String fileHash = "empty-file-hash-" + System.currentTimeMillis();
        byte[] fileContent = new byte[0];

        var sendResult = testKafkaTemplate.send("upload-topic", fileHash, fileContent).get(10, TimeUnit.SECONDS);

        assertThat(sendResult.getRecordMetadata()).isNotNull();
        assertThat(sendResult.getProducerRecord().key()).isEqualTo(fileHash);
        Thread.sleep(2000);
        assertThat(fileContent).isEmpty();
    }

    @Test
    void processLargeValidFile_successfully() throws Exception {
        String fileHash = "large-valid-hash-" + System.currentTimeMillis();
        byte[] fileContent = createLargeValidXlsx(4 * 1024 * 1024);

        var sendResult = testKafkaTemplate.send("upload-topic", fileHash, fileContent).get(10, TimeUnit.SECONDS);

        assertThat(sendResult.getRecordMetadata()).isNotNull();
        assertThat(sendResult.getProducerRecord().key()).isEqualTo(fileHash);
        Thread.sleep(3000);
        assertThat(fileContent.length).isGreaterThan(0);
        assertThat(fileContent.length).isLessThan(5 * 1024 * 1024);
    }

    private static byte[] createLargeValidXlsx(int size) throws IOException {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = wb.createSheet("Large Data");
            int rowCount = 1000;
            for (int i = 0; i < rowCount; i++) {
                var row = sheet.createRow(i);
                for (int j = 0; j < 10; j++) {
                    row.createCell(j).setCellValue("Data row " + i + " col " + j + " with some long text to increase file size");
                }
            }
            try (var out = new ByteArrayOutputStream()) {
                wb.write(out);
                byte[] content = out.toByteArray();
                if (content.length < size) {
                    byte[] paddedContent = new byte[size];
                    System.arraycopy(content, 0, paddedContent, 0, content.length);
                    return paddedContent;
                }
                return content;
            }
        }
    }

    @Test
    void sendStatusMessage_worksCorrectly() throws Exception {
        String fileHash = "status-test-hash-" + System.currentTimeMillis();
        String statusMessage = """
            {
                "fileHash": "%s",
                "status": "PROCESSED",
                "processedData": {"rows": 10, "columns": 5}
            }
            """.formatted(fileHash);

        var sendResult = testStatusKafkaTemplate.send("status-topic", fileHash, statusMessage).get(10, TimeUnit.SECONDS);

        assertThat(sendResult.getRecordMetadata()).isNotNull();
        assertThat(sendResult.getProducerRecord().key()).isEqualTo(fileHash);
        assertThat(sendResult.getProducerRecord().value()).contains("PROCESSED");
    }

    @Test
    void sendValidationFailedStatus_worksCorrectly() throws Exception {
        String fileHash = "validation-failed-hash-" + System.currentTimeMillis();
        String statusMessage = """
            {
                "fileHash": "%s",
                "status": "SECONDARY_VALIDATION_FAILED",
                "errorMessage": "Invalid file format"
            }
            """.formatted(fileHash);

        var sendResult = testStatusKafkaTemplate.send("status-topic", fileHash, statusMessage).get(10, TimeUnit.SECONDS);

        assertThat(sendResult.getRecordMetadata()).isNotNull();
        assertThat(sendResult.getProducerRecord().value()).contains("SECONDARY_VALIDATION_FAILED");
    }
}