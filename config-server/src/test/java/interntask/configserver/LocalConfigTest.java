package interntask.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LocalConfigTest {

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Test
    void configServerLoadsConfiguration() {
        Environment env = environmentRepository.findOne("file-uploader", "local", null);

        assertThat(env).isNotNull();
        assertThat(env.getPropertySources()).isNotEmpty();
        assertThat(env.getName()).isEqualTo("file-uploader");
        assertThat(env.getProfiles()).contains("local");
    }

    @Test
    void fileUploaderConfigExists() throws IOException {
        Resource resource = new ClassPathResource("config-repo/file-uploader-local.yml");
        assertThat(resource.exists()).isTrue();

        String content = Files.readString(Paths.get(resource.getURI()));

        // Исправляем проверки согласно реальному содержимому файла
        assertThat(content).contains("max-size: 5242880"); // 5MB в байтах
        assertThat(content).contains("allowed-extensions: xls, xlsx");
        assertThat(content).contains("port: 8081");
        assertThat(content).contains("database: firestarter_db");
    }

    @Test
    void configContainsValidProperties() throws IOException {
        Resource resource = new ClassPathResource("config-repo/file-uploader-local.yml");
        String content = Files.readString(Paths.get(resource.getURI()));

        // Проверяем структуру YAML
        assertThat(content).contains("app:");
        assertThat(content).contains("file:");
        assertThat(content).contains("server:");
        assertThat(content).contains("spring:");
        assertThat(content).contains("kafka:");
    }
}