package wf.garnier.spring.security.configurer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigurerApplicationTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void noHeader() {
        mvc.get()
                .uri("/private")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasHeader("WWW-Authenticate", "Basic realm=\"Realm\"");
    }

    @Test
    void withRobotHeader() {
        mvc.get()
                .uri("/private")
                .header("x-robot-secret", "beep-boop")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.OK)
                .bodyText()
                .contains("Ms Robot");
    }

    @Test
    void withRobotHeaderWrongSecret() {
        mvc.get()
                .uri("/private")
                .header("x-robot-secret", "beep-beep")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.FORBIDDEN);
    }

}
