package wf.garnier.spring.security.authenticationprovider;

import java.io.IOException;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlPasswordInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationProviderApplicationTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient.getCookieManager().clearCookies();
    }

    @Test
    void httpBasicDaniel() {
        mvc.get()
                .uri("/private")
                .with(httpBasic("daniel", "foobar"))
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void httpBasicAlice() {
        mvc.get()
                .uri("/private")
                .with(httpBasic("alice", "alice-password"))
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void httpBasicAliceWrongPassword() {
        mvc.get()
                .uri("/private")
                .with(httpBasic("alice", "foobar"))
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void formLoginDaniel() throws IOException {
        HtmlPage page = webClient.getPage("/private");

        page.<HtmlInput>querySelector("#username").type("daniel");
        page.<HtmlPasswordInput>querySelector("#password").type("some random password");
        page = page.<HtmlButton>querySelector("button").click();

        assertThat(page.getBody().getTextContent()).contains("daniel");
    }

    @Test
    void formLoginAlice() throws IOException {
        HtmlPage page = webClient.getPage("/private");

        page.<HtmlInput>querySelector("#username").type("alice");
        page.<HtmlPasswordInput>querySelector("#password").type("alice-password");
        page = page.<HtmlButton>querySelector("button").click();

        assertThat(page.getBody().getTextContent()).contains("alice");
    }

    @Test
    void formLoginAliceWrongPassword() throws IOException {
        HtmlPage page = webClient.getPage("/private");

        page.<HtmlInput>querySelector("#username").type("alice");
        page.<HtmlPasswordInput>querySelector("#password").type("some random password");
        page = page.<HtmlButton>querySelector("button").click();

        assertThat(page.getUrl()).hasPath("/login").hasParameter("error");
        assertThat(page.getBody().getTextContent()).contains("Bad credentials");
    }

}
