# Spring Security Workshop: Using `AuthenticationProvider`s

In the previous module, we discovered how to implement a filter that performs an authentication
flow.

But what if all we really want is to customize the way credentials are verified? And we don't care
how they are extracted from the request? This is where the `AuthenticationProvider`s come in: we let
Spring Security transform the request into an un-authenticated `Authentication` object, and we only
provide the logic to validate credentials.

## The use-case

Our app is getting more and more secure. I mean, a `beep-boop` secret? Go and hack that!

But it is also gaining traction, and so now we need an admin to. You know. Administer. Things. And
we've on-boarded this lovely fellow named Daniel, kind, rigorous, competent... But he has a bad
memory. He can't even remember is password.

So we will implement a special security measure, just for him - when someone tries to log in, and
their username is `daniel` ... don't check the password, just log them in[^1].

Feel free to replace `daniel` by whatever name you want, I won't bear grudge 😁

## The starting app

We are starting from the app that was built in the previous module. You can either continue from the
project your started in the previous module (recommended), or, if you're a bit behind or would like
to start fresh, start from the project that is in this directory.

If you use the app in this directory, note that we use Dex with docker-compose as the SSO identity
provider. If you haven't, you can remove the `org.springframework.boot:spring-boot-docker-compose`
dependency in `build.gradle`, and re-use your `application.yml` file from the previous module.

## Step 0: verify that the app runs

As a reminder, you can run the app from the command-line:

```bash
./gradlew bootRun
```

You can also run from your favorite IDE.

With the current setup of the app, you can log-in with:

- Form login:
  - alice / alice-password
  - bob / bob-password
- Dex
  - admin@example.com / password

A filter has been registered that blocks requests with a `x-verboden: waar` header, called
`VerbodenFilter`.

A robot authentication has been implemented, you can now obtain private pages by passing the
`x-robot-secret: beep-boop` header, like so:
`curl localhost:8080/private -H "x-robot-secret: beep-boop"`

## Step 1: create and register an authentication provider

No filters this time - extracting the credentials from the username-password login is already
handled by Spring Security, here the `UsernamePasswordAuthenticationFilter`.

We will implement the
[AuthenticationProvider](https://docs.spring.io/spring-security/site/docs/6.1.5/api/org/springframework/security/authentication/AuthenticationProvider.html)
interface. Call it `DanielAuthenticationProvider` and make sure it only support
`UsernamePasswordAuthenticationToken` classes - this avoids triggering this provider on non
username-password flows, e.g. on OAuth2 login or our custom Robot login.

Then, in the `authenticate` method, check the name of the incoming authentication. If it's daniel,
return an "authenticated" version of
[UsernamePasswordAuthenticationToken](https://docs.spring.io/spring-security/site/docs/6.1.5/api/org/springframework/security/authentication/UsernamePasswordAuthenticationToken.html).
Look up the javadoc, there are useful static methods for creating what you want. I recommend you use
the `User` class as the principal (but it can be any object)! You should have built users in your
`SecurityConfiguration`, so you could do the same here. Don't worry too much about required
passwords, you can put anything, it will be erased at login time.

If the username is not Daniel, you want to delegate to the default authentication provider, in this
case it's a
[DaoAuthenticationProvider](https://docs.spring.io/spring-security/site/docs/6.1.5/api/org/springframework/security/authentication/dao/DaoAuthenticationProvider.html)
that Spring Security creates for you. Remember, you do not need to be explicit about calling another
auth provider, you should let the framework do the heavy lifting.

Then register this `AuthenticationProvider` in your security configuration - try to guess which
method to call to register an authentication provider 🕵️

---

<details>

<summary>📖 solution</summary>

DanielAuthenticationProvider.java:

```java
public class DanielAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication != null && "daniel".equals(authentication.getName())) {
            var daniel = User.withUsername("daniel")
                    .password("<will be erased>")
                    .roles("user", "admin")
                    .build();
            return UsernamePasswordAuthenticationToken.authenticated(daniel, null, daniel.getAuthorities());
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
```

SecurityConfiguration.java:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ...
                .authenticationProvider(new DanielAuthenticationProvider())
                .build();
    }

    // ...
}
```

</details>

---

Now try to login with `daniel` and any password. The app should let you log in.

Try again with `alice` and any password: the app should reject you, as it applies the default form
login credentials-checking.

That's very useful: you didn't have to implement a filter to extract credentials from the request
yourself, get them from the body, etc etc. You just verify they are correct.

This has many useful implications, which we'll discover in the following steps.

## Step 2: enabling HTTP basic

There are other ways to log in with a username and password, like HTTP basic. Spring Security
supports http basic out of the box, activate it in the filter chain with
`.httpBasic(Customizer.withDefaults())`.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ...
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    // ...
}
```

You can then request the private page with a raw HTTP call, e.g. with curl:

```bash
curl localhost:8080/private --user daniel:anypasswordyouwant
# should succeed

curl localhost:8080/private --user alice:anypasswordyouwant
# should fail

curl localhost:8080/private --user alice:alice-password
# should succeed
```

Again - very neat, there's a spring-security managed way to extract credentials from a request,
different from form login (in this case,
[HTTP Basic auth](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication#basic_authentication_scheme)),
and, you don't need to do anything to support it!

As a developer, you provide code to validate credentials, only.

## (Optional) Step 3: more benefits of using `AuthenticationProvider`s

There are other benefits to using the `AuthenticationProvider` interface. For example, you get
[Observability](https://docs.spring.io/spring-security/reference/servlet/integrations/observability.html#observability-tracing)
out of the box. Some extra protection mesaures, such as credential erasures. You also get
[authentication events](https://docs.spring.io/spring-security/reference/servlet/authentication/events.html).

Register a new `@Bean` providing an `ApplicationListener<AuthenticationSuccessEvent>`, that logs the
name and the class of the `Authentication` object when someone logs in. You can add it in the
`SecurityConfiguration`, for example. Feel free to use `@EventListener` instead if you are more
familiar with it[^1].

---

<details>

<summary>📖 SecurityConfiguration.java</summary>

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    // ...

    @Bean
    public ApplicationListener<AuthenticationSuccessEvent> listener() {
        var logger = LoggerFactory.getLogger("🔐 custom-security-logger");

        return event -> {
            var auth = event.getAuthentication();
            logger.info(
                    "[{}] logged in as [{}]",
                    auth.getName(),
                    auth.getClass().getSimpleName());
        };
    }
}
```

</details>

---

Try logging in with:

1. Username / password (and/or http-basic)
2. OpenID (dex or otherwise)
3. Robot authentication

Notice that for the first two cases, you get a log event, but for the robot authentication you
don't. That's because our custom Robot filter does not leverage the `AuthenticationManager`
interface. We will introduce it in one of the last modules, if we have time.

## Closing out

Finally, you have encountered all the authentication building blocks in Spring Security. At least in
Servlet mode; Reactive is not super different, it has `Filter`s but they are `WebFilter`s with
reactive types instead, and its own reactive `AuthenticationManager`.

In the next module, we'll dive deeper into overloading existing behavior, rather than implementing
stuff from scratch.

[^1]:
    To learn more about Spring Framework eventing support, check out the
    [reference docs](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
