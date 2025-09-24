package interntask.fileuploader.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "fileStatuses")
public class FileStatus {
    @Id
    private String fileHash;
    private String status; // "ACCEPTED", "VALIDATION_FAILED", "PROCESSED"
    private String message;

    public FileStatus() {}
    public FileStatus(String fileHash, String status, String message) {
        this.fileHash = fileHash;
        this.status = status;
        this.message = message;
    }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
