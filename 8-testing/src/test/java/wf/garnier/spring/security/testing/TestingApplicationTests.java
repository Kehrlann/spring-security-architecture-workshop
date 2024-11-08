package wf.garnier.spring.security.testing;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

@SpringBootTest
@AutoConfigureMockMvc
class TestingApplicationTests {

	@Autowired
	MockMvcTester mvc;

	@Test
	void publicPage() {
		var result = mvc.get().uri("/");

		//@formatter:off
		assertThat(result)
				.hasStatus(HttpStatus.OK)
				.bodyText()
				.contains("<h1>Hello world!</h1>");
		//@formatter:on;
	}

	@Test
	void forbiddenFilter() {
		// You can also assert everything fluently by calling `.assertThat()` on the
		// result of `.exchange()`
		mvc.get()
			.uri("/")
			.header("x-forbidden", "true")
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.FORBIDDEN)
			.bodyText()
			.isEqualTo("⛔⛔⛔⛔ this is forbidden");
	}

	@Test
	void privatePageAnonymous() {
		//@formatter:off
		mvc.get()
			.uri("/private")
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.UNAUTHORIZED);
		//@formatter:on
	}

	@Test
	@WithMockUser("test-user")
	void privatePageWithUser() {
		mvc.get()
			.uri("/private")
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.OK)
			.bodyText()
			.contains("Hello, ~[test-user]~ !");
	}

	@Test
	@WithMockUser(value = "test-user", roles = { "user" })
	void adminPageNotAuthorized() {
		mvc.get()
			.uri("/admin")
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.FOUND)
			.redirectedUrl()
			.isEqualTo("/private?denied");
	}

	@Test
	@WithMockUser(value = "test-user", roles = { "user", "admin" })
	void adminPageAuthorized() {
		mvc.get()
			.uri("/admin")
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.OK)
			.bodyText()
			.contains("This is a very serious admin section. Only admins can see this page.");
	}

	@Test
	void authenticationDaniel() {
		mvc.get()
			.uri("/private")
			.with(SecurityMockMvcRequestPostProcessors.httpBasic("daniel", "lol-i-can-type-anything"))
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.OK)
			.bodyText()
			.contains("Hello, ~[daniel]~ !");
	}

	@Test
	void authenticationAlice() {
		mvc.get()
			.uri("/private")
			.with(SecurityMockMvcRequestPostProcessors.httpBasic("alice", "password"))
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.OK)
			.bodyText()
			.contains("Hello, ~[alice]~ !");
	}

	@Test
	void authenticationAliceWrongPassword() {
		mvc.get()
			.uri("/private")
			.with(SecurityMockMvcRequestPostProcessors.httpBasic("alice", "wrong-password"))
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void formLoginDaniel() {
		mvc.post()
			.uri("/login")
			.param("username", "daniel")
			.param("password", "foobar")
			.with(SecurityMockMvcRequestPostProcessors.csrf())
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.FOUND)
			.redirectedUrl()
			.isEqualTo("/private");
	}

	@Test
	void formLoginAlice() {
		mvc.post()
			.uri("/login")
			.param("username", "alice")
			.param("password", "password")
			.with(SecurityMockMvcRequestPostProcessors.csrf())
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.FOUND)
			.redirectedUrl()
			.isEqualTo("/private");
	}

	@Test
	void formLoginAliceWrongPassword() {
		mvc.post()
			.uri("/login")
			.param("username", "alice")
			.param("password", "wrong-password")
			.with(SecurityMockMvcRequestPostProcessors.csrf())
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.FOUND)
			.redirectedUrl()
			.isEqualTo("/login?error");
	}

	@Test
	void ssoLogin() {
		mvc.get()
			.uri("/private")
			.with(oidcLogin().idToken(token -> token.claim("email", "test@example.com")))
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.OK)
			.bodyText()
			.contains("Hello, ~[test@example.com]~ !");
	}

}
