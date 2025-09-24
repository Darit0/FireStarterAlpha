package interntask.fileuploader.service;

import interntask.fileuploader.exception.FileValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class FileValidationService {
    @Value("${app.file.max-size}")
    private long maxSizeMb;

    @Value("${app.file.allowed-extensions}")
    private String allowedExtensionsStr;

    public void validateFile(MultipartFile file) {
         //Проверка размера
        if (file.getSize() > maxSizeMb) {
            throw new FileValidationException("FILE_TOO_LARGE",
                    "File size exceeds " + maxSizeMb + " Bytes or 5 Mb");
        }
        //На всякий и с пустым именем файлы
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new FileValidationException("INVALID_FILENAME", "Filename is empty");
        }

        //Проверка расширений
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot == -1) {
            throw new FileValidationException("NO_EXTENSION", "File has no extension");
        }

        String ext = originalFilename.substring(lastDot + 1).toLowerCase();
        Set<String> allowed = Arrays.stream(allowedExtensionsStr.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        if (!allowed.contains(ext)) {
            throw new FileValidationException("UNSUPPORTED_EXTENSION",
                    "Only " + allowed + " extensions are allowed");
        }
    }
}
