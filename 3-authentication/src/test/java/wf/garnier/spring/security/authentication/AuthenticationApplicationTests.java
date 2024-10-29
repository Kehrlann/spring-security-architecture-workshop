package wf.garnier.spring.security.authentication;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationApplicationTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void noHeader() {
        mvc.get()
                .uri("/private")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .redirectedUrl()
                .endsWith("/login");
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
