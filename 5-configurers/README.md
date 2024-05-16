# Spring Security Workshop: Wiring it all together with Configurers

In the previous modules, we have implemented authentication using `Filter`s that interact with the
HTTP request/response, and then a simpler way with Spring Security's managed `Authentication`s.

We've seen that our filter doesn't benefit from all the "goodness" from the authentication
architecture, because it does not use an `AuthenticationProvider`.

We will refactor our filter, introduce an `AuthenticationProvider`, and showcase how Spring Security
configures its own filter chains.

## The use-case

There is no specific new feature requested, we "just" want things to be wired up the way Spring
Security does it.

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

A filter has been registered that blocks requests with a `x-forbidden: true` header, called
`ForbiddenFilter`.

A robot authentication has been implemented, you can now obtain private pages by passing the
`x-robot-secret: beep-boop` header, like so:
`curl localhost:8080/private -H "x-robot-secret: beep-boop"`

You can log in with HTTP basic, e.g. `curl localhost:8080/private --user "alice:alice-password"`.

Lastly, you can use the username `daniel` with any password.

## Step 1: refactor the `RobotAuthenticationToken`

As we've seen with the previous module, when calling `Authentication.isAuthenticated()`, it may
return either true or false. When it returns true, just like our `RobotAuthenticationToken` object,
it represents an "authenticated" entity. When it returns `false`, it's an "authentication request",
a "bag of credentials", that should be authenticated by an authentication manager, most likely with
an authentication provider.

Refactor the `RobotAuthenticationToken`, so that there are two possible states:

1. An "authenticated" state:
   - `isAuthenticated()` must return true
   - `getCredentials()` must return null
   - it must have the `ROLE_robot` authority
1. An "unauthenticated" state (new):
   - `isAuthenticated()` must return false
   - `getCredentials()` must return a string, that will passed in when creating the object - it will
     contain the "robot secret"
   - it must have no authorities

Optional recommendations - don't follow those if you don't wan't to:

1. Make sure that these objects are immutable
1. Make the constructor private, and use two factory (static) methods, e.g.
   `RobotAuthenticationToken.authenticated()` and
   `RobotAuthenticationToken.unauthenticated(String password)`
1. You may add a `String getSecret()` method or change `getCrendentials()` to return a `String`

Update the filter to use the "authenticated" version.

---

<details>

<summary>📖 solution</summary>

RobotAuthenticationToken.java:

```java
public class RobotAuthenticationToken extends AbstractAuthenticationToken {

    private final boolean authenticated;

    private final String secret;

    private RobotAuthenticationToken() {
        super(AuthorityUtils.createAuthorityList("ROLE_robot"));
        this.authenticated = true;
        this.secret = null;
    }

    private RobotAuthenticationToken(String secret) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.authenticated = false;
        this.secret = secret;
    }

    public static RobotAuthenticationToken authenticated() {
        return new RobotAuthenticationToken();
    }

    public static RobotAuthenticationToken unauthenticated(String secret) {
        return new RobotAuthenticationToken(secret);
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        throw new RuntimeException("I am immutable!");
    }

    @Override
    public String getCredentials() {
        return secret;
    }

    @Override
    public Object getPrincipal() {
        return "Ms Robot 🤖";
    }

}
```

RobotAuthenticationFilter.java:

```java
public class RobotAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // ...
        var newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(RobotAuthenticationToken.authenticated());
        SecurityContextHolder.setContext(newContext);

        filterChain.doFilter(request, response);
    }

}
```

</details>

---

The usual http login with a `x-robot-secret` header should still work, e.g.
`curl localhost:8080/private -H "x-robot-secret: beep-boop"`, but still shouldn't produce any logs.

## Step 2: Add an `AuthenticationProvider`

Create an `AuthenticationProvider`, that only supports `RobotAuthenticationToken` (see previous
module), and checks that the secret / credentials is `beep-boop`. If it's not the case, throw an
[AuthenticationException](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/AuthenticationException.html) -
you should be able to find a suitable exception subclass for this use-case.

No need to register it in the filter chain just yet: our filter doesn't use an
`AuthenticationManager`.

---

<details>

<summary>📖 RobotAuthenticationProvider.java</summary>

```java
public class RobotAuthenticationProvider implements AuthenticationProvider {

    private final String secret = "beep-boop";

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var authrequest = (RobotAuthenticationToken) authentication;
        if (secret.equals(authrequest.getCredentials())) {
            return RobotAuthenticationToken.authenticated();
        }
        throw new BadCredentialsException("🤖⛔️ you are not Ms Robot");

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RobotAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
```

</details>

---

## Step 3: Wire it up in the `RobotAuthenticationFilter`

Now we want this filter to be used. We will start by hardcoding how the filter gets its
`AuthenticationManager`.

Update the `RobotAuthenticationFilter`, so that it has a private field of type
`AuthenticationManager`, and the value will be a
[ProviderManager](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/authentication/ProviderManager.html).
Make sure wire in the correct `AuthenticationProvider`.

Then, use the `authenticationManager` in the `doFilterInternal` method, by way of an unauthenticated
`RobotAuthenticationToken`. There should be a try-catch somewhere, with rejecting the "reject the
request" logic in the catch block. In the end, there should be no password comparison in the filter
anymore.

---

<details>

<summary>📖 RobotAuthenticationFilter.java</summary>

```java
public class RobotAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROBOT_HEADER_NAME = "x-robot-secret";

    private final AuthenticationManager authenticationManager = new ProviderManager(new RobotAuthenticationProvider());

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!Collections.list(request.getHeaderNames()).contains(ROBOT_HEADER_NAME)) {
            filterChain.doFilter(request, response);
            return; // make sure to skip the rest of the filter logic
        }

        var secret = request.getHeader(ROBOT_HEADER_NAME);
        var authRequest = RobotAuthenticationToken.unauthenticated(secret);

        try {
            var authentication = authenticationManager.authenticate(authRequest);
            var newContext = SecurityContextHolder.createEmptyContext();
            newContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(newContext);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            // These two lines are required to have emojis in your responses.
            // See ForbiddenFilter for more information.
            response.setCharacterEncoding(StandardCharset.UTF_8.name());
            response.setContentType("text/plain;charset=utf-8");

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write(exception.getMessage());
            response.getWriter().close(); // optional

            // We're not calling into the rest of the filter chain here
        }

    }

}
```

</details>

---

Try the usual calls to make sure the reject still gets through or gets rejected depending on the
secret, e.g. `curl localhost:8080/private -H "x-robot-secret: beep-boop"` or with password
`boop-beep`.

Notice that we are not getting any logs: that's because we are not using Spring Security's provided
authentication manager, that's registered with the filter chain. In order to get this, we must use a
"configurer".

## Step 4: Building a configurer

The filter chain has its own mini-version of a Spring "Application Context" - you can access "shared
objects" in the `HttpSecurity` builder object. To access those, one must use a
[custom DL](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html#jc-custom-dsls).
As you can see in the link above, those are subclasses of `AbstractHttpConfigurer<T, HttpSecurity>`,
and should override two methods - `init` and `configure`. Those classes are commonly referred to as
"configurers", and are what you register when you call `.formLogin()` or `.oauth2Login()` on
`HttpSecurity`.

Spring Security will call the `init` method of all registered configurers in sequence, and the call
the `configure` method. The `init` method will be used to register all elements that do not require
dependencies, typically: `AuthenticationProvider`s. The `configure` method will be used to register
elements that have dependencies.

In the case of `HttpSecurity`, between the `init` and `configure` steps, the `AuthenticationManager`
will be built, with all registered `AuthenticationProvider`s, and set as a shared object. See
`AbstractConfiguredSecurityBuilder#doBuild` for more details.

Your task here is to create it to create a `RobotConfigurer`, inspired by the explanations above, or
by info you will find in
[this section of a blog post](https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter#accessing-the-local-authenticationmanager).

In the `init` method, register the `AuthenticationProvider`. In the `configure` method, grab the
`AuthenticationManager` from the shared objects of the builder, create a
`RobotAuthenticationFilter`, pass it the `AuthenticationManager`, and the put the filter in the
chain (you will need to update the Filter so that you can pass the auth manager in the constructor).

Finally, update your security configuration, by removing the direct reference to the filter, an
`.apply()`'ing your newly created configurer. For historical reasons, you must call `.apply()`
outside of the fluent API.

That's quite a lot!

---

<details>

<summary>📖 solution</summary>

RobotConfigurer.java:

```java
public class RobotConfigurer extends AbstractHttpConfigurer<RobotConfigurer, HttpSecurity> {

    @Override
    public void init(HttpSecurity http) {
        http.authenticationProvider(new RobotAuthenticationProvider());
    }

    @Override
    public void configure(HttpSecurity http) {
        var authManager = http.getSharedObject(AuthenticationManager.class);
        var filter = new RobotAuthenticationFilter(authManager);
        http.addFilterBefore(filter, AuthorizationFilter.class);
    }
}
```

RobotAuthenticationFilter.java:

```java
public class RobotAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    public RobotAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // ...
}
```

SecurityConfiguration.java

```java
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.apply(new RobotConfigurer());
        return http. //...
    }
}
```

</details>

---

Now, when you try to log in using the robot account, you'll actually see a log!

```
2023-11-03T03:00:35.150+01:00  INFO 10643 --- [nio-8080-exec-1] 🔐 custom-security-logger                : [Ms Robot 🤖] logged in as [RobotAuthenticationToken]
```

## Closing out

So why is this important? Because that's how Spring Security does things. Check out for example
`FormLoginConfigurer`, `OAuth2LoginConfigurer`, `CsrfConfigurer`. By looking at those, you should be
able to discover which `Filter`s and `AuthenticationProvider`s are registered, and how you can
extend them.
