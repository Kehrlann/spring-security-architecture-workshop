package wf.garnier.spring.security.testing;

import java.io.IOException;

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
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SsoIntegrationTests {

	private static final DexContainer.User ALICE = new DexContainer.User("alice", "alice@corp.example.com", "password");

	private static final DexContainer.User BOB = new DexContainer.User("bob", "bob@example.com", "password");

	@ServiceConnection
	@Container
	private static final DexContainer dexContainer = new DexContainer(
			DexContainer.DEFAULT_IMAGE_NAME.withTag(DexContainer.DEFAULT_TAG))
		.withUser(ALICE)
		.withUser(BOB);

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

		dexLoginPage.<HtmlInput>getElementByName("login").type(ALICE.email());
		dexLoginPage.<HtmlPasswordInput>getElementByName("password").type(ALICE.clearTextPassword());

		HtmlPage appPage = dexLoginPage.getElementById("submit-login").click();

		assertThat(appPage.getBody().getTextContent()).contains(ALICE.email());
	}

	@Test
	void forbiddenDomain() throws IOException {
		webClient.getOptions().setRedirectEnabled(true);

		HtmlPage loginPage = webClient.getPage("http://localhost:%s/private".formatted(port));

		HtmlPage dexLoginPage = loginPage.<HtmlAnchor>querySelector("table > tbody > tr > td > a").click();

		dexLoginPage.<HtmlInput>getElementByName("login").type(BOB.email());
		dexLoginPage.<HtmlPasswordInput>getElementByName("password").type(BOB.clearTextPassword());

		HtmlPage appPage = dexLoginPage.getElementById("submit-login").click();

		assertThat(appPage.getUrl()).hasPath("/login").hasParameter("error");

		var errorPopup = appPage.querySelector("div[role=\"alert\"]");
		assertThat(errorPopup.getTextContent()).contains(
				"Cannot log in because email has domain [@example.com]. Only emails with domain [corp.example.com] are accepted.");
	}

}
