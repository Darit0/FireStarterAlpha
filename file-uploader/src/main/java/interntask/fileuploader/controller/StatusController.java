package interntask.fileuploader.controller;

import interntask.fileuploader.repository.FileStatusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/status")
public class StatusController {

    private final FileStatusRepository statusRepository;

    public StatusController(FileStatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @GetMapping("/{hash}")
    public ResponseEntity<?> getFileStatus(@PathVariable String hash) {
        return statusRepository.findByFileHash(hash)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
