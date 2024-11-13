# Spring Security Workshop: Testing

Testing is one of the central tenets of modern software development. You can't get away with no testing - and it is a
good thing!

But adding security to an application means adding _conditions_ that must be met before being able to access an endpoint
or calling a method. In general, this means more setup in your tests. Fortunately, Spring Security has built in support
for simple testing.

## The use-case

We are going to apply three authorization rules, that we added in the previous module.

1. Only users with the role `admin` can view the `/admin` page. This will showcase basic HTTP
   request security.
1. Users can log in with SSO using Dex, but only `@corp.example.com` e-mail addresses are allowed,
   other domains cannot log in. For example, e-mails ending in `@example.com` will not be able to
   log in.

## The starting app

We are starting from a blend of various things we did the in the previous modules.

First, we'll have form-login with three users:

- `alice`, with password `password` and role `admin`
- `bob`, with password `password`, a regular user
- `daniel`, who can log in with any password (see below), and has role `admin`

If you run the application with the `docker` profile, you'll be able to use SSO login with Dex as well.

Run with:

```bash
SPRING_PROFILES_ACTIVE=docker ./gradlew :8-testing:bootRun
```

or

```bash
./gradlew :8-testing:bootRun --args='--spring.profiles.active=docker'
```

This means you can log in with Dex, but there following the rules we implemented in module 6: only `@corp.example.com`
e-mail addresses are log-in, so `*@example.com` are not allowed. Using Dex:

- `alice@corp.example.com` can log in with `password`
- `bob@example.com` can log in into Dex `password`, but they will not be allowed to log in into our app

We have three pages:

- `/`: public, anyone can view
- `/private`: private, must be authenticated to view
- `/admin`: must have role `admin` to view, so only alice can view the page. Non-admins will be redirected to
  `/private`.

There is the `ForbiddenFilter` from module 2. Making a request with a header `X-Forbidden: true` returns HTTP 403, no
matter the page.

We also have the `RobotAuthenticationFilter` from module 3. Requests to `/private` are allowed when sending the
`X-Robot-Secret: beep-boop` header, which authenticates the request as coming from our robot-user.

The `DanielAuthenticationProvider` from module 4 is also active, which allows users to authenticate using the username
`daniel` with any password. This is valid for both HTML-based form login (navigating to `/login`) and for HTTP-basic
authentication.

## Step 0: verify that the app runs

As a reminder, you can run the app from the command-line:

```bash
./gradlew :8-testing:bootRun
```

Essentially, you want to try ou the following:

1. Form login with alice, bob and daniel
2. Login with dex with `alice@corp.example.com` and `bob@example.com`
3. HTTP-basic with alice, bob and daniel
4. HTTP request to `/` with `x-forbidden: true`
5. HTTP request to `/private` with `x-robot-secret: beep-boop`

## Step 1: setting up the tools

First, we want to validate that getting the `/` page even works, and exercise our testing tools. For fun and profit,
we'll use the new tools introduced in Boot 3.4:
`MockMvcTester` ([Boot docs](https://docs.spring.io/spring-boot/3.4/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.with-mock-environment)).
It is essentially a newer version of the `MockMvc` utility, with fluent methods and `assertJ` support. Take a look at
the [Spring reference docs](https://docs.spring.io/spring-framework/reference/6.2/testing/mockmvc/assertj.html) on
`MockMvcTester`, focusing on "Performing Requests" and "Defining expectations". Think of `MockMvc(Tester)` as a
test-focused curl or Postman. It does not behave like a browser, e.g. does not send `Origin` or `User-Agent` headers.

For our first test, we'll ensure that the `/` page returns `HTTP 200 OK`, and that the response contains
`<h1>Hello world!</h1>`. Update the `TestingApplicationTests` to implement this first test.

---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

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
}
```

</details>

---

You can then run the tests either from IDE, or directly from the command line with `./gradlew 8-testing:test`.

## Step 2: Testing the forbidden filter

The easiest filter to test is the `ForbiddenFilter`, we just need to add a header to the request. Ensure that passing
`x-forbidden: true` when getting the `/` page does return an `HTTP 403 Forbidden` response.

---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

@SpringBootTest
@AutoConfigureMockMvc
class TestingApplicationTests {

    // ...

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

}
```

</details>

---

## Step 3: Basic testing with users

The private page should be private, and only accessible to authenticated users. We could try and make requests to the
login page, type the username in the field, the password, and so on. But that would both add a lot of code to our tests,
and be very inconvenient.

Spring Security has annotations to help you inject users in your `MockMvc*`
tests: [@WithMockUser](https://docs.spring.io/spring-security/reference/servlet/test/method.html#test-method-withmockuser).

Using `@WithMockUser`, implement a test that shows that a user called `test-user` can access the `/private` page and
have their name on the page. Also write the opposite test, showcasing that trying to access the private page with "no
user" returns an error. Since `MockMvcTester` behaves like curl, you'll get and `HTTP 401 Unauthorized`, not a redirect
to the login page. This is due to using `.httpBasic()` in our security configuration.

---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
class TestingApplicationTests {

    // ...

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

}
```

</details>

---

Notice here that we aren't using any of the existing users, `alice`, `bob` or `daniel`. Instead, we're making a "mock"
user, that only really exists in the scope of the test using it.

Lastly, we would like to make sure that our check on the `admin` role works. Discover how you can configure mock user to
include roles or authorities by looking at the available values in `@WithMockerUser`, and write to tests trying to
access the `/admin` page. If the test user has the correct role `admin`, they should get the page. If they don't, they
should get redirected to the `/private?denied` page - there are special methods in the `MockMvcTester` result to test
for redirect urls.

---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

@SpringBootTest
@AutoConfigureMockMvc
class TestingApplicationTests {

    // ...

    @Test
    @WithMockUser(value = "test-user", roles = {"user"})
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
    @WithMockUser(value = "test-user", roles = {"user", "admin"})
    void adminPageAuthorized() {
        mvc.get()
                .uri("/admin")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.OK)
                .bodyText()
                .contains("This is a very serious admin section. Only admins can see this page.");
    }

}
```

</details>

---

## Step 4: Testing authentication

All the tests above do not actually exercise _authentication_. They touch filters, and check _authorization_
(permissions), but they do not try to log in. Most of the time, we don't need to check the login process - after all, we
trust Spring Security to do the right thing for us.

This is, unless we have some authentication-related specific code: a custom login page, or our
`DanielAuthenticationProvider` for example. We want to ensure that `daniel` can log in with any password, but that
`alice` must use her real password. The easiest way to check authentication code in our case is to rely on HTTP basic
auth. Read up on
[SecurityMockMvcRequestPostProcessors](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/request-post-processors.html),
and how you can use them with `MockMvc*`. Then write three tests accessing the `/private` page:

- One for `daniel` with any password
- One for `alice` with the correct password
- One for `alice` with the incorrect password

---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;


@SpringBootTest
@AutoConfigureMockMvc
class TestingApplicationTests {

    // ...


    @Test
    void authenticationDaniel() {
        mvc.get()
                .uri("/private")
                .with(httpBasic("daniel", "lol-i-can-type-anything"))
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
                .with(httpBasic("alice", "password"))
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
                .with(httpBasic("alice", "wrong-password"))
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

}
```

</details>

---

Notice that we're getting `401 Unauthorized` when the password is incorrect - this is how Spring Security handles HTTP
basic authentication failures.

Let's level up, and try form-login based authentication. Try and write a test that tests login with `daniel` and any
password by making a POST request to `/login`. You can pass the `username` and `password` parameters to `MockMvcTester`
by using the `.param("username", "daniel")` method. The response should be HTTP 302 Found, redirecting the user to
`/private`.

---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

@SpringBootTest
@AutoConfigureMockMvc
class TestingApplicationTests {

    // ...

    @Test
    void formLoginDaniel() {
        mvc.post()
                .uri("/login")
                .param("username", "daniel")
                .param("password", "foobar")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .redirectedUrl()
                .isEqualTo("/private");
    }

}
```

</details>

---

But if you write a test like this, it will fail and redirect the user back to `/login?error`. Try and figure out why, by
reading the debug logs from Spring Security. Turn them on by adding the following to your `application.yaml`:

```yaml
logging:
  level:
    org.springframework.security: TRACE
```

When you have found the issue (hint: it has to do with protecting POST request from cross-site attacks), think for a
second how you would fix it. The most obvious way would be to use a browser-based testing tool, or at least parse the
HTML response and extract the information we need from it. This is very inconvenient, and that's why there is a
`SecurityMockMvcRequestPostProcessors` for this specific use-case! Find the correct post-processor, and fix the test.
You can also add some tests for form-login for `alice`, one with the correct password and one with an incorrect
password.

---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class TestingApplicationTests {

    // ...

    @Test
    void formLoginDaniel() {
        mvc.post()
                .uri("/login")
                .param("username", "daniel")
                .param("password", "foobar")
                .with(csrf())
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
                .with(csrf())
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
                .with(csrf())
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.FOUND)
                .redirectedUrl()
                .isEqualTo("/login?error");
    }

}
```

</details>

---

> 🧑‍🔬 If you would like to learn more about Cross-Site Request Forgery attacks, the reference docs has a nice chapter on
> [how the attack works](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html); and, as usual
> when it comes to security, the [OWASP website](https://owasp.org/www-community/attacks/csrf) is one of the most
> complete resources out there.

## Step 4: Testing with SSO users

So far, we've only run our tests with in-memory / database-backed users, using Spring Security's `UserDetails` under the
hood. When you have non-trivial authentications, there are custom `SecurityMockMvcRequestPostProcessors` to inject any
authentication types, e.g. `SecurityMockMvcRequestPostProcessors#authentication`. We've seen `httpBasic()` already.
Another common use-case is SSO, which you can achieve with `oicLogin()`. Find out more
in [Testing OAuth2 > Testing OIDC login](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#testing-oidc-login),
then write a test showing the `/private` page with a user logging in with OIDC and the e-mail address
`test@example.com`.

---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

@SpringBootTest
@AutoConfigureMockMvc
class TestingApplicationTests {

    // ...


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
```

</details>

---

Again, this does not test the _authentication_ mechanism, only the authorization after authorization. So the address
`@example.com` is not rejected!

## Step 5: Full integration testing with SSO and Dex

> ⚠️ This section introduces many concepts at once, and may be complicated to implement. Depending on whether you've
> used these things before or not, it may be hard to write the tests by yourself - but it's fine! The idea is to show
> what's possible, rather than have you implement everything.

> 🧑‍🔬 We have already imported all the required dependencies in `build.gradle`. Take a look if you are curious!

To test our `DomainAwareOidcService` which only allows addresses like `@corp.example.com`, we need to interact with an
OpenID identity provider. We could use an online service, such as Okta or Microsoft Entra, but usually we don't want our
tests to touch the network. We could also use a local Keycloak, but we would like to avoid setting up things on your dev
machine just to run your tests.

So the easiest solution is to reach for containerized services - like we do with the `docker` profile. But rather than
launching those containers manually, we can use [Testcontainers](https://testcontainers.com/), which offers a Java API
to start and configure containers. For this, we're going to use Dex, similar to our Docker-Compose integration, because
it starts much faster than Keycloak.

First, read up
on [Spring Boot's Testcontainers integration](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html#page-title).
start by trying to add a Dex container to your tests, using Testcontainer's `GenericContainer<?>`. At least
ensure that the Dex container starts and stops with your tests, even if your Boot application does not connect to it
yet.

If you're feeling really adventurous, try and configure Dex manually using Testcontainer's `GenericContainer<?>`, but
the lifecycle is complicated: there is a circular dependency between Dex and your application, where Dex needs to know
the port your app runs on before starting, and your app need to know Dex's port before starting. This can be solved by
fixing the ports, which we don't recommend: it's better to avoid conflicts by having both containers and tests run on
random ports. If you'd like a more "curated" experience, you are free to use
my [testcontainers-dex](https://github.com/Kehrlann/testcontainers-dex) module, which you can also find on
the [Testcontainers modules catalog](https://testcontainers.com/modules/). It has a Spring Boot integration, which
allows you to use `@ServiceConnection` directly.

For this integration to work, your application under test MUST be exposed over HTTP, and therefore use a full Tomcat
webserver, not just a MockMvc environment. Furthermore, you cannot use `MockMvc*` as you are not using the Mock
environment. I recommend using [HtmlUnit](https://htmlunit.org/) for a lightweight web-client experience, without having
to driver a real browser through Selenium. Take a look at
their [getting started guide](https://htmlunit.org/gettingStarted.html).

Create a new test file, for example `SsoIntegrationTests`, ensure your `@SpringBootTest` has the correct
`webEnvironment` set up. Using Testcontainers-Dex + HTML-Unit, write two tests that shows that a user with an address in
`@example.com` can't log in, but a user with an address ending in
`@corp.example.com` can. If you need inspiration, you should take a look at the samples in
my [Testcontainers-Dex](https://github.com/Kehrlann/testcontainers-dex/blob/main/sample-spring/README.md) module.


---

<details>

<summary>📖 TestingApplicationTests.java</summary>

```java

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlPasswordInput;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import wf.garnier.testcontainers.dexidp.DexContainer;

import java.io.IOException;

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
```

</details>

---

And with this, you have a full integration test that leverages a "real" identity provider! Under the hood, there is
some lifecycle management, while using a raw container might be complicated - but that's what libraries are for, after
all.

## Closing out

With this, we close our tour of Security-focused testing. There are not many classes involved, the core being the
`@WithMockUser` and `SecurityMockMvcRequestPostProcessors`. Head over to
the [Spring Security testing docs](https://docs.spring.io/spring-security/reference/reactive/test/index.html) if you are
curious about the other things the library has to offer.

Testing security is important. Disabling security just for tests is a terribly bad idea. It leads to slip-ups and
un-secured apps shipped to production. It is much better to test your security code, and pay the small overhead to have
security enabled in your business logic tests as well.

