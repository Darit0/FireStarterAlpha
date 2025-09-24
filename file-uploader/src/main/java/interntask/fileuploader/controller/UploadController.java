package interntask.fileuploader.controller;

import interntask.fileuploader.dto.ErrorResponse;
import interntask.fileuploader.exception.FileValidationException;
import interntask.fileuploader.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private final FileUploadService uploadService;

    public UploadController(FileUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileHash = uploadService.handleFileUpload(file);
            return ResponseEntity.ok(fileHash);
        } catch (FileValidationException e) {
            ErrorResponse error = new ErrorResponse(e.getErrorType(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}