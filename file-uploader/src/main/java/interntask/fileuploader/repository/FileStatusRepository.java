package interntask.fileuploader.repository;

import interntask.fileuploader.dto.FileStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileStatusRepository extends MongoRepository<FileStatus, String> {
    Optional<FileStatus> findByFileHash(String fileHash);
}
