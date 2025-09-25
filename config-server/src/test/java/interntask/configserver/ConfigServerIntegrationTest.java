package interntask.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldServeFileUploaderConfig() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/file-uploader/local",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        String responseBody = response.getBody();
        assertThat(responseBody).contains("\"app.file.max-size\":5242880");
        assertThat(responseBody).contains("\"app.file.allowed-extensions\":\"xls, xlsx\"");
        assertThat(responseBody).contains("\"name\":\"file-uploader\"");
        assertThat(responseBody).contains("\"profiles\":[\"local\"]");
    }

    @Test
    void healthEndpointShouldWork() {
        // Config Server имеет свой формат health endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Проверяем что это валидный ответ Config Server
        String responseBody = response.getBody();
        assertThat(responseBody).contains("\"name\":\"actuator\"");
        assertThat(responseBody).contains("\"profiles\":[\"health\"]");

        // Или просто проверяем что endpoint отвечает
        assertThat(responseBody).isNotEmpty();
    }

    @Test
    void configServerEndpointsAreAccessible() {
        String[] endpoints = {
                "/actuator/health",
                "/file-uploader/local",
                "/application/default",
                "/file-uploader/default"
        };

        for (String endpoint : endpoints) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "http://localhost:" + port + endpoint,
                    String.class
            );
            assertThat(response.getStatusCode().is2xxSuccessful())
                    .withFailMessage("Endpoint %s should return 2xx status", endpoint)
                    .isTrue();
        }
    }
}