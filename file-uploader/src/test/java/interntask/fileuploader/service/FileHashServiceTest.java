package interntask.fileuploader.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FileHashServiceTest {

    private final FileHashService service = new FileHashService();

    @Test
    void computeHash_returnsConsistentSha256() {
        byte[] content = "Hello, world!".getBytes();
        String hash1 = service.computeHash(content);
        String hash2 = service.computeHash(content);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 = 64 hex chars
    }

    @Test
    void computeHash_differentContent_givesDifferentHash() {
        byte[] content1 = "A".getBytes();
        byte[] content2 = "B".getBytes();
        assertThat(service.computeHash(content1))
                .isNotEqualTo(service.computeHash(content2));
    }
}