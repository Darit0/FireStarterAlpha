package interntask.fileprocessor.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class JsonUtilTest {

    @Test
    void escapeJson_handlesSpecialChars() {
        String input = "He said \"Hello!\nHow are you?\"";
        String escaped = JsonUtil.escapeJson(input);
        assertThat(escaped).isEqualTo("He said \\\"Hello!\\nHow are you?\\\"");
    }
}