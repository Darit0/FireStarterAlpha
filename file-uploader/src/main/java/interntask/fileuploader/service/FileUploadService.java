package interntask.fileuploader.service;

import interntask.fileuploader.dto.FileStatus;
import interntask.fileuploader.exception.FileValidationException;
import interntask.fileuploader.repository.FileStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    private final FileValidationService validationService;
    private final FileHashService hashService;
    private final FileStatusRepository statusRepository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final KafkaTemplate<String, String> statusKafkaTemplate;

    @Value("${kafka.topics.upload:upload-topic}")
    private String uploadTopic;

    @Value("${kafka.topics.status:status-topic}")
    private String statusTopic;

    public FileUploadService(FileValidationService validationService,
                             FileHashService hashService,
                             FileStatusRepository statusRepository,
                             KafkaTemplate<String, byte[]> kafkaTemplate,
                             KafkaTemplate<String, String> statusKafkaTemplate) {
        this.validationService = validationService;
        this.hashService = hashService;
        this.statusRepository = statusRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.statusKafkaTemplate = statusKafkaTemplate;
    }

    public String handleFileUpload(MultipartFile file) {
        //Валидация
        validationService.validateFile(file);

        //Хеш
        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new FileValidationException("IO_ERROR", "Failed to read file content");
        }
        String fileHash = hashService.computeHash(content);

        //Проверка дубликата
        var existing = statusRepository.findByFileHash(fileHash);
        if (existing.isPresent()) {
            FileStatus status = existing.get();
            if ("PROCESSED".equals(status.getStatus())) {
                // Уже успешно обработан — возвращаем хеш
                return fileHash;
            }
            //Если статус не "PROCESSED", то можно повторить
        }

        //Сохраняем статус "ACCEPTED"
        FileStatus newStatus = new FileStatus(fileHash, "ACCEPTED", "File passed initial validation");
        statusRepository.save(newStatus);

        //Отправляем в Kafka
        kafkaTemplate.send(uploadTopic, fileHash, content);
        statusKafkaTemplate.send(statusTopic, fileHash, """
            {"fileHash":"%s","status":"ACCEPTED","message":"File passed initial validation"}""".formatted(fileHash));

        return fileHash;
    }
}