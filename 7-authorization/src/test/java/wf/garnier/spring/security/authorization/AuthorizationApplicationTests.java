package wf.garnier.spring.security.authorization;

import java.io.IOException;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlPasswordInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import wf.garnier.testcontainers.dexidp.DexContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc // you can have BOTH random port AND MockMvc at the same time ;)
@Testcontainers
class AuthorizationApplicationTests {

    // Here we are using Testcontainers + https://github.com/Kehrlann/testcontainers-dex/
    @ServiceConnection
    @Container
    static DexContainer container = new DexContainer(DexContainer.DEFAULT_IMAGE_NAME.withTag(DexContainer.DEFAULT_TAG));

    @Autowired
    MockMvcTester mvc;

    @LocalServerPort
    private int port;

    // Here we do not autowire a WebClient with @WebMvcTest, because that client
    // can only talk to the Spring app, and wouldn't work with the Dex login page.
    private final WebClient webClient = new WebClient();


    @Nested
    class AdminPage {

        @Test
        @WithMockUser(username = "test-user-with-a", authorities = "ROLE_admin")
        void valid() {
            mvc.get()
                    .uri("/admin")
                    .exchange()
                    .assertThat()
                    .hasStatus(HttpStatus.OK);
        }

        @Test
        @WithMockUser(username = "test-user-with-a")
        void missingRole() {
            mvc.get()
                    .uri("/admin")
                    .exchange()
                    .assertThat()
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl("/private");
        }

        @Test
        @WithMockUser(username = "wrong-user", authorities = "ROLE_admin")
        void unauthorizedUsername() {
            mvc.get()
                    .uri("/admin")
                    .exchange()
                    .assertThat()
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl("/private");
        }

        @Test
        @WithMockUser(username = "test-user-with-a", authorities = { "ROLE_admin", "ROLE_geoguesser" })
        void showsVenues() {
            mvc.get()
                    .uri("/admin")
                    .exchange()
                    .assertThat()
                    .hasStatus(HttpStatus.OK)
                    .bodyText()
                    .contains("Devoxx Belgium (Kinepolis)");
        }

        @Test
        @WithMockUser(username = "test-user-with-a", authorities = { "ROLE_admin" })
        void hidesVenues() {
            mvc.get()
                    .uri("/admin")
                    .exchange()
                    .assertThat()
                    .hasStatus(HttpStatus.OK)
                    .bodyText()
                    .doesNotContain("Kinepolis");
        }

    }

    @Nested
    class OAuthPage {

        @BeforeEach
        void setUp() {
            webClient.getCookieManager().clearCookies();
        }

        @Test
        void oauthLogin() throws IOException {
            webClient.getOptions().setRedirectEnabled(true);

            HtmlPage loginPage = webClient.getPage("http://localhost:%s/oauth".formatted(port));

            HtmlPage dexLoginPage = loginPage.<HtmlAnchor>querySelector("table > tbody > tr > td > a").click();

            dexLoginPage.<HtmlInput>getElementByName("login").type(container.getUser().email());
            dexLoginPage.<HtmlPasswordInput>getElementByName("password").type(container.getUser().clearTextPassword());

            HtmlPage appPage = dexLoginPage.getElementById("submit-login").click();

            assertThat(appPage.getBody().getTextContent()).contains(container.getUser().email());
        }

        @Test
        void formLogin() throws IOException {
            webClient.getOptions().setRedirectEnabled(true);

            HtmlPage loginPage = webClient.getPage("http://localhost:%s/oauth".formatted(port));

            loginPage.<HtmlInput>querySelector("#username").type("alice");
            loginPage.<HtmlPasswordInput>querySelector("#password").type("some random password");

            HtmlPage loginWithErrorPage = loginPage.<HtmlButton>querySelector("button").click();
            assertThat(loginWithErrorPage.getUrl()).hasPath("/login").hasParameter("error");

            var errorPopup = loginWithErrorPage.querySelector("div[role=\"alert\"]");
            assertThat(errorPopup.getTextContent()).contains("Bad credentials");
        }

    }


}
