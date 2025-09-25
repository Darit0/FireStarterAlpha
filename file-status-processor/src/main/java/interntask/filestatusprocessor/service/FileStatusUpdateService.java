package interntask.filestatusprocessor.service;

import interntask.filestatusprocessor.dto.FileStatus;
import interntask.filestatusprocessor.repository.FileStatusRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileStatusUpdateService {

    private static final Logger log = LoggerFactory.getLogger(FileStatusUpdateService.class);

    private final FileStatusRepository repository;

    public FileStatusUpdateService(FileStatusRepository repository) {
        this.repository = repository;
    }

    public void updateStatus(String fileHash, String status, String message) {
        try {
            FileStatus fileStatus = new FileStatus(fileHash, status, message);
            repository.save(fileStatus);
            log.info("Updated status for file {}: {}", fileHash, status);
        } catch (Exception e) {
            log.error("Failed to update status for file " + fileHash, e);
            throw e;
        }
    }
}