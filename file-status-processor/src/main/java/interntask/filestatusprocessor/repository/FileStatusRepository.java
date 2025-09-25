package interntask.filestatusprocessor.repository;


import interntask.filestatusprocessor.dto.FileStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileStatusRepository extends MongoRepository<FileStatus, String> {
}
