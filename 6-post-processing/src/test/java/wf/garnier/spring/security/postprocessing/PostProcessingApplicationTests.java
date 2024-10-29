package wf.garnier.spring.security.postprocessing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlPasswordInput;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import wf.garnier.testcontainers.dexidp.DexContainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PostProcessingApplicationTests {

    private static final DexContainer.User REGULAR_USER =
            new DexContainer.User("test-user", "test-user@example.com", "password");

    private static final DexContainer.User CORP_USER =
            new DexContainer.User("test-user-corp", "test-user@corp.example.com", "password");

    // Here we are using Testcontainers + https://github.com/Kehrlann/testcontainers-dex/
    @ServiceConnection
    @Container
    static DexContainer container = new DexContainer(DexContainer.DEFAULT_IMAGE_NAME.withTag(DexContainer.DEFAULT_TAG))
            .withUser(REGULAR_USER)
            .withUser(CORP_USER);

    @LocalServerPort
    private int port;

    // Here we do not autowire a WebClient with @WebMvcTest, because that client
    // can only talk to the Spring app, and wouldn't work with the Dex login page.
    private final WebClient webClient = new WebClient();

    @Test
    void allowedDomain() throws IOException {
        webClient.getOptions().setRedirectEnabled(true);

        HtmlPage loginPage = webClient.getPage("http://localhost:%s/private".formatted(port));

        HtmlPage dexLoginPage = loginPage.<HtmlAnchor>querySelector("table > tbody > tr > td > a").click();

        dexLoginPage.<HtmlInput>getElementByName("login").type(CORP_USER.email());
        dexLoginPage.<HtmlPasswordInput>getElementByName("password").type(CORP_USER.clearTextPassword());

        HtmlPage appPage = dexLoginPage.getElementById("submit-login").click();

        assertThat(appPage.getBody().getTextContent()).contains(CORP_USER.email());
    }

    @Test
    void forbiddenDomain() throws IOException {
        webClient.getOptions().setRedirectEnabled(true);

        HtmlPage loginPage = webClient.getPage("http://localhost:%s/private".formatted(port));

        HtmlPage dexLoginPage = loginPage.<HtmlAnchor>querySelector("table > tbody > tr > td > a").click();

        dexLoginPage.<HtmlInput>getElementByName("login").type(REGULAR_USER.email());
        dexLoginPage.<HtmlPasswordInput>getElementByName("password").type(REGULAR_USER.clearTextPassword());

        HtmlPage appPage = dexLoginPage.getElementById("submit-login").click();

        assertThat(appPage.getUrl()).hasPath("/login").hasParameter("error");

        var errorPopup = appPage.querySelector("div[role=\"alert\"]");
        assertThat(errorPopup.getTextContent())
                .contains("Cannot log in because email has domain [@example.com]. Only emails with domain [corp.example.com] are accepted.");
    }

}
