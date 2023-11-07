# Spring Security Workshop: Getting Started with Spring Security

This first module is intended to get you warmed up with Spring Security, and to establish a base
application that will be used in subsequent modules. The goal is NOT to build a realistic
application, so we will be taking many shortcuts.

If you've never used Spring Security before, this is a good place to get started.

If you are already familiar with basic Spring Security setups, you should breeze through this
section, but going through it will give you a good example of what can be achieved.

## Simple Security

We will start with an extremely basic, two-page application. One page should be "public", i.e.
accessible to anyone, and one page should be private, i.e. accessible only to logged-in users. In
this section, we will focus on "authentication" (= "logging in", "knowing who logged in"), but we
will not touch "authorization" (= "permissions", "what is this user allowed to see").

## Step 1: running the existing application

The base application is a simple Spring MVC application, run through Spring Boot. It has a
controller, and a few templates. Take a look at the `GreetingsController` and notice two pages:

- `/`: the public page
- `/private`: the private page

For now, there is no security, so run the application and navigate to the public page. You can run
the application through your favorite IDE, or through the command-line using gradle:

```bash
./gradlew bootRun
```

The app will run locally, on port 8080: http://localhost:8080/

The app uses
[Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools),
and [Thymeleaf templates](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html) which
are refreshed as you develop, so, most of the time, you should not have to re-run the application
for your changes to take effect. Try it out, change a template, refresh your browser page and see
the changes.

> 🧑‍🔬 We have prepared the `/private` page to a logout button ... but only if the "login" feature is
> implemented! When you think about it, it makes sense: you can't really "log in" yet, so you
> shouldn't be able to "log out" either.
>
> Under the hood, Thymeleaf checks whether a `_csrf` attribute is present in the Request attributes,
> and, if so, displays the log out button.

## Step 2: adding the Spring Security dependency

Now we want to make the `/private` page actually private. To do this, we will bring in Spring
Security. Update the `dependencies` section Gradle configuration, in `build.gradle`:

```gradle
dependencies {
    implementation "org.springframework.boot:spring-boot-starter-thymeleaf"
    implementation "org.springframework.boot:spring-boot-starter-web"
    implementation "org.springframework.boot:spring-boot-starter-security"
    developmentOnly "org.springframework.boot:spring-boot-devtools"
    testImplementation "org.springframework.boot:spring-boot-starter-test"
}
```

You will have to reload your Gradle project, and restart your application. Spring Boot Devtools
doesn't bode well with dependency changes.

Once this is done, navigate back to the main page: http://localhost:8080/ ... you should see a form
login, with username and password fields. This is a default Spring Security configuration that
Spring Boot applies automatically.

> 🧑‍🔬 To learn more, check out the Spring Boot docs -
> [8.4 Spring Security](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.security)

One of the principles of Spring Security is that everything should be secure by default, including
the "public" page. To log in, inspect the logs of your application, you should see something like
this:

```
2023-10-13T14:48:39.341+02:00  WARN 69981 --- [  restartedMain] .s.s.UserDetailsServiceAutoConfiguration :

Using generated security password: 9b471373-360c-4d6f-9665-b0b0b9c7a264

This generated password is for development use only. Your security configuration must be updated before running your application in production.
```

The default user is `user`, and the password is randomly generated generated every time you start
the application. Try logging in, and see both the public and private page. Notice that you can now
"log out" on the private page.

Going to the console every time the app restart to copy the password is inconvenient, so instead we
will hardcode the password. Look up the
[Spring Boot docs](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.security)
on how to set up a hardcoded user / password.

---

<details>

<summary>📖 solution</summary>

Update your `application.yml` file, and add:

```yaml
spring:
  security:
    user:
      name: "user"
      password: "password"
```

You can now log in with `user` / `password`.

</details>

---

## Step 3: make the `/` page public

We want the public page to be accessible to anyone, but the private page to only be accessible to
authenticated users. This is an "authorization" decision, albeit a very simple one: we decide "who"
has permission to access the public and private page.

Configuration for Spring Security happens through a `SecurityFilterChain`. We will explore how this
works later in this workshop, but if you want to learn more by yourself, there is a dedicated
section in the Spring Security docs -
[Authorization architecture](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html)

We will create a configuration class with a SecurityFilterChain bean:

```java
@Configuration
@EnableWebSecurity
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(
						authorizeHttp -> {
                            // Here, use authorizeHttp to make "/" public and "/private" private
                            // Take a look at the Spring Security docs for this:
                            // https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#authorize-requests
						}
				)
				.build();
	}
}
```

You may need to restart your application when adding this class.

Once this is done, verify that:

1. The public page is accessible: http://localhost:8080/
   1. If styling seems to be missing ... make sure the CSS, hosted under `/css/`, is accessible by
      anyone
   1. Open the network tab in your browser. Are there other failing requests? Fix those too.
1. The private page is not accessible: http://localhost:8080/private
   1. If you get a blank page, take a look at the network tab, and see that you are getting an
      `HTTP - 403 forbidden` response. To re-enable the Spring whitelabel error page, make sure the
      `/error` endpoint is also accessible.

---

<details>

<summary>📖 Full configuration</summary>

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/").permitAll();
                    authorize.requestMatchers("/css/**").permitAll();
                    authorize.requestMatchers("/error").permitAll();
                    authorize.requestMatchers("/favicon.svg").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .build();
    }
}
```

</details>

---

Notice something different about the private page though? While we do get a `HTTP 403 - Forbidden`,
we have lost our ability to log in! In fact, navigating to http://localhost:8080/login will give you
an error. This is because we have disabled the entire auto-configuration provided by Spring Boot,
including the form login capabilities.

## Step 4: enable form login

Let's bring form login back in - you can find how to in the
[Spring Security docs](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html).
Use the most basic, default configuration.

---

<details>

<summary>📖 Form login enabled</summary>

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/").permitAll();
                    authorize.requestMatchers("/css/**").permitAll();
                    authorize.requestMatchers("/error").permitAll();
                    authorize.requestMatchers("/favicon.svg").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .formLogin(Customizer.withDefaults())
                .build();
    }
}
```

</details>

---

Try the app again, you should be able to log in and out.

Bonus: when you log out and log back in, you are redirect to the public page, or `/`. Configure form
login so that, when you log in, you are taken to the private page. Go back to the docs, or explore
the form login customizations through your editor.

---

<details>

<summary>📖 Redirect to the private page on successful login</summary>

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            // ... instead, use:
            .formLogin(form -> {
                form.defaultSuccessUrl("/private");
            })
            .build();
}
```

</details>

---

## (Optional) Step 4b: add multiple users

So far we have used Spring Boot's auto-configuration to capabilities to create a user and assign a
password. But what if we wanted more users?

Add two users, `alice` and `bob`, by following the
[In-Memory Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/in-memory.html)
section of the Spring Security docs.

---

<details>

<summary>📖 Add `alice` and `bob`</summary>

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/").permitAll();
                    authorize.requestMatchers("/css/**").permitAll();
                    authorize.requestMatchers("/error").permitAll();
                    authorize.requestMatchers("/favicon.svg").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .formLogin(form -> {
                    form.defaultSuccessUrl("/private");
                })
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        var userFactory = User.withDefaultPasswordEncoder();
        var alice = userFactory.username("alice")
                .password("alice-password")
                .build();
        var bob = userFactory.username("bob")
                .password("bob-password")
                .build();
        return new InMemoryUserDetailsManager(alice, bob);
    }
}
```

</details>

---

Notice how this overrides whatever configuration you put in your `application.yml`: Spring Boot
backs off and lets you provide your own beans.

## Step 5: display the username

We have handled authentication ("login") and authorization ("private page == must be logged in"). We
would like to use the information we have about the user in the private page, to greet them nicely.
This means our `GreetingsController#getName` must return some real value, rather than a hardcoded
`"UNKNOWN USER"`.

One way to get the current user is through the
[@AuthenticationPrincipal](https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html#mvc-authentication-principal)
annotation for Spring `@Controller` method arguments. We will not use this here, for reasons that
will be come apparent later on.

Instead we'll inject an `org.springframework.security.core.Authentication` argument into our
controller method, like so:

```java
@Controller
class GreetingsController {

	@GetMapping("/private")
	public String privatePage(Authentication authentication, Model model) {
		model.addAttribute("name", getName());
		return "private";
	}

}
```

How can you obtain the username, and display it? Change the `getName()` method to accept the
`authentication` argument, and use it inside.

---

<details>

<summary>📖 Display username</summary>

```java
@Controller
class GreetingsController {
	@GetMapping("/")
	public String publicPage() {
		return "public";
	}

	@GetMapping("/private")
	public String privatePage(Authentication authentication, Model model) {
		model.addAttribute("name", getName(authentication));
		return "private";
	}

	private static String getName(Authentication authentication) {
		return authentication.getName();
	}

}
```

</details>

---

## Step 6: support different authentication schemes

We have implemented form login, with one or two users. We have authentication and authorization. One
of the nice properties of Spring Security is that it abstracts away the details of how
authentication is performed, and then provides you an `Authentication` object to work with.

In this step, we are going to implement an "SSO" login using OpenID Connect ("openid" or "oidc" for
short). It is one of the most popular standards for implementing SSO. It is built on top of OAuth2,
so it's not big stretch to also cover "authorization" use-cases with OAuth2's access tokens. Again,
the goal of this section is not to teach you how OpenID works - if you want to learn more, a quick
search on the web will bring up many good introductions. (Shameless plug: I gave a live-coding
session on precisely this at [Devoxx Belgium 2023](https://www.youtube.com/watch?v=wP4TVTvYL0Y)).

The most common self-hosted openid provider is [Keycloak](https://www.keycloak.org/). While Keycloak
is extremely versatile and powerful, it is also resource-intensive, so instead we will leverage the
very lean [Dex](https://dexidp.io/)[^1].

Of course, there is a host of online alternatives (Google, Azure AD, Okta, Auth0, Ping Identity,
...). There even is a
[list of "certified" openid providers](https://openid.net/certification/#OPENID-OP-P).

First and foremost, you need to enable "Spring Security OAuth2", and fortunately Spring Boot has a
starter for that, that you can add into your Gradle file:
`org.springframework.boot:spring-boot-starter-oauth2-client`. Do not forget to reimport your Gradle
dependencies.

---

<details>

<summary>📖 Changes to build.gradle</summary>

```gradle
dependencies {
    implementation "org.springframework.boot:spring-boot-starter-thymeleaf"
    implementation "org.springframework.boot:spring-boot-starter-web"
    implementation "org.springframework.boot:spring-boot-starter-security"
    // vv add this vv
    implementation "org.springframework.boot:spring-boot-starter-oauth2-client"
    // ^^ add this ^^
    developmentOnly "org.springframework.boot:spring-boot-devtools"
    testImplementation "org.springframework.boot:spring-boot-starter-test"
}
```

</details>

---

Second, you need to enable `oauth2login` in your filter chain. Using code completion, try and enable
oauth2 login in your `SecurityConfig`.

---

<details>

<summary>📖 Update `SecurityConfig` with `oauth2login`</summary>

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ...
                .oauth2Login(Customizer.withDefaults())
                .build();
    }

    // ...
}
```

</details>

---

If you start your application, this should fail because Spring Boot cannot find a
`ClientRegistration`. There are three options for creating a `ClientRegistration`, depending on
which identity provider you would like to use.

---

### (Option 1) Using Google

One of the "easy" providers is Google, as it comes pre-configured in Spring Security and has
[a section of the reference doc using Google as an example](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html#oauth2login-sample-initial-setup).

---

<details>

<summary>📖 Update application.yml</summary>

```yaml
spring:
  # ...
  security:
    # ...
    oauth2:
      client:
        registration:
          google:
            client-id: <client-id> # copied when you created your Google client
            client-secret: <client-secret> # copied when you created your Google client
```

</details>

---

Try logging in into your with your Google credentials now. It should work.

### (Option 2) Using Dex, locally

You may notice that there is a `docker-compose.yml` file in the root of this directory, which
launches `dex`. You could run it manually by doing `docker-compose up` or `docker-compose up -d` to
launch it in the background.

Instead, we will leverage Spring Boot's support for Docker, by adding
[the appropriate dependency](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.docker-compose).

---

<details>

<summary>📖 Changes to build.gradle</summary>

```gradle
dependencies {
    implementation "org.springframework.boot:spring-boot-starter-thymeleaf"
    implementation "org.springframework.boot:spring-boot-starter-web"
    implementation "org.springframework.boot:spring-boot-starter-security"
    implementation "org.springframework.boot:spring-boot-starter-oauth2-client"
    // vv add this vv
    developmentOnly "org.springframework.boot:spring-boot-docker-compose"
    // ^^ add this ^^
    developmentOnly "org.springframework.boot:spring-boot-devtools"
    testImplementation "org.springframework.boot:spring-boot-starter-test"
}
```

</details>

---

Then run your app, and verify that dex is running by either doing `docker ps` or vising the OpenID
Configuration Discovery endpoint, http://localhost:5556/.well-known/openid-configuration, which
should return an HTTP 200 with a valid JSON response.

Then, you need to make this a Spring Security `ClientRegistration`. You can achieve that through
Spring Boot properties:

```yaml
spring:
  # ...
  security:
    # ...
    oauth2:
      client:
        registration:
          dex:
            client-id: base-client
            client-secret: base-secret
            client-name: Login with Dex
            scope:
              - openid
              - email
        provider:
          dex:
            issuer-uri: http://localhost:5556
```

The values above are all taken from the Dex configuration, in `dex.yml`, except for the `scope`
which comes from the openid specification.

The username / password you will use to log in using this provider is `admin@example.com` /
`password`.

### (Option 3) Using an online service

If you know you way around an online service and want to use it, you may register a client there.

Use the redirect URI `http://localhost:8080/login/oauth2/code/<REGISTRATION_ID>`, where
`<REGISTRATION_ID>` is any alphanumeric string you want, representing that provider (I recommend
something like `okta` for Okta, `auth0` for Google, etc). When the registration is complete, you
will obtain a `client_id` and a `client_secret`. Take note of them. You must also find the
`issuer_uri` for your auth server, usually described somewhere in the UI. The username and password
will be anything you use to login with that service.

Then, you need to make this a Spring Security `ClientRegistration`. You can achieve that through
Spring Boot properties:

```yaml
spring:
  # ...
  security:
    # ...
    oauth2:
      client:
        registration:
          <REGISTRATION_ID>: # change this
            client-id: <client_id> # change this
            client-secret: <client_secret> # change this
            client-name: Login with <REGISTRATION_ID>
            scope:
              - openid
              - email
        provider:
          <REGISTRATION_ID>: # change this
            issuer-uri: <issuer_uri> # change this
```

The username / password will be the one you used to log in into your authentication provider.

## Step 7: Displaying the user's e-mail when logging in with SSO

Now, you should be able to log in with openid. However, the name of the user displayed on the page
probably looks like a random string. We would like to display the e-mail instead. Change the
`GreetingsController#getName(Authentication)` method to show the e-mail. In order to find the
e-mail, run your application in debug mode, and then inspect the `Authentication.principal` object.

> 🚨 Your app should still as before when you log in with username / password.
> `GreetingsController#getName` should be be able to handle both cases.

---

<details>

<summary>📖 Display email</summary>

```java
@Controller
class GreetingsController {

	//...

	private static String getName(Authentication authentication) {
		if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
			return oidcUser.getEmail();
		}
		return authentication.getName();
	}

}
```

</details>

---

## (Optional) Step 8: Redirect to the private on successful OpenID login

When you log out and log back in using OpenID, which page are you redirected on?, you are redirect
to the public page, or `/`. Configure form login so that, when you log in, you are taken to the
private page. Go back to the docs, or explore the form login customizations through your editor.

## (Optional) Step 9: Add a second OpenID provider

Re-try the above, but with a second type of OpenID provider.

---

## Closing out

We have secured a very simple application, implementing both username/password login and SSO. The
app is not prod-ready, but the goal of this demo is not to make you use all the Spring Security
features!

We will now discuss how Spring Security achieves what we have implemented so far.

[^1]:
    Dex is intended to provide a "federated" identity provider that can act as an "auth proxy"
    between your app and many identity providers. Think of it as an identity proxy, or some sort of
    "[Anti-Corruption Layer](https://softwareengineering.stackexchange.com/questions/184464/what-is-an-anti-corruption-layer-and-how-is-it-used)".
    We won't be using those capabilities here, and instead use it in "standalone" mode, which is
    only intended for local development.
