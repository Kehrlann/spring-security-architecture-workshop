# Spring Security Workshop: Adding authentication

In this module, we will leverage our previous knowledge of filters to implement authentication. That
means we will get credentials, verify them, and then produce a Spring Security `Authentication`
object.

## The use-case

We have a very useful app, with two pages, one public and one private. We got a new request from our
Ops team, they want to monitor that the private page behaves correctly. They could automate a flow
that gets the login page, then tries to type things into boxes, then grabs the session cookie, etc
etc. That would be inconvenient - let's come up with a custom workflow for them[^1].

When a request comes in, we will grab a custom header, `x-robot-secret`, and when the value equals
`beep-boop`, we will create a "Robot" `Authentication` object and store it in the SecurityContext.

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
`VerbodenFilter`. It's a good start to start implementing an authentication filter.

## Step 1: create and register a filter

Again we will create a filter, and then register it in the filter chain. Start by repeating the
steps we have applied in the previous module, to create and register a filter. For now, the filter
does nothing, you can add a log to it if you want.

Call the filter `RobotAuthenticationFilter`.

When you register it in the filter chain, be sure to register it BEFORE `AuthorizationFilter.class`.

---

<details>

<summary>📖 solution</summary>

RobotAuthenticationFilter.java:

```java
public class RobotAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        System.out.println("~~~~> 🤖 Hello from RobotAuthenticationFilter!");
        filterChain.doFilter(request, response);
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
                .addFilterBefore(new RobotAuthenticationFilter(), AuthorizationFilter.class)
                .build();
    }

    // ...
}
```

</details>

---

Do a "smoke test": make sure you can still log in as usual, and that the log shows up in the
console.

## Step 2: verifying the `x-robot-secret` header

In the filter, grab the value of the `x-robot-secret` header, and display it in the log. If the
secret is not equal to `beep-boop`, reject the request, similar to what we have done in the previous
filter - maybe change the message to something more "robot-y".

---

<details>

<summary>📖 RobotAuthenticationFilter.java</summary>

```java
public class RobotAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var secret = request.getHeader("x-robot-secret");

        if (!"beep-boop".equals(secret)) {
            // These two lines are required to have emojis in your responses.
            // See VerbodenFilter for more information.
            response.setCharacterEncoding(StandardCharset.UTF_8.name());
            response.setContentType("text/plain;charset=utf-8");

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("🤖⛔️ you are not Ms Robot");
            response.getWriter().close(); // optional

            // Absolutely make sure you don't call the rest of the filter chain!!
            return;
        }

        filterChain.doFilter(request, response);
    }

}
```

</details>

---

This is very similar to what we have done in the previous chapter, so you can test it the same way,
with curl, httpie, etc.

Notice that if you try and visit the app in a browser, it won't work anymore, because the browser is
not sending a `x-robot-secret` header. That's normal, we will think about this later on.

## Step 3: performing authentication

Now we have checked that the password is not incorrect, but that doesn't give access to the private
page: if you try to access `/private`, you'll get redirected to `/login`, e.g. with httpie:

```shell
http :8080/private x-robot-secret:beep-boop
# HTTP/1.1 302
# Cache-Control: no-cache, no-store, max-age=0, must-revalidate
# Connection: keep-alive
# Content-Length: 0
# Date: Thu, 26 Oct 2023 12:21:13 GMT
# Expires: 0
# Keep-Alive: timeout=60
# Location: http://localhost:8080/login
# Pragma: no-cache
# Set-Cookie: JSESSIONID=4F14FF70A222AD7F458CD773C15D3EC5; Path=/; HttpOnly
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# X-XSS-Protection: 0
```

Remember, we're trying to grant access to the private page. To achieve this, when the
`x-robot-secret` header value is correct, we will create an `Authentication`.

First, create an implementation of `org.springframework.security.core.Authentication`. Call it
`RobotAuthenticationToken` - it's a convention in Spring Security, see
`UsernamePasswordAuthenticationToken` for example.

If you implement `Authentication` directly, you'll see that you need to override a lot of methods
that may not make much sense to you. Instead, we can use an abstract base class:
[AbstractAuthenticationToken](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/authentication/AbstractAuthenticationToken.html)

The following rules must apply:

1. Credentials must be null
2. "Authenticated" must be true
3. The principal must be a String, for example `Ms Robot`. Note: it could be anything but we will
   use the simplest thing we can.
4. Authorities must be a list with only `ROLE_robot` as a value. Note: it could be a list containing
   anything, or even "no authorities" (empty list), but it can't be null.
   - Authorities represent "high-level permissions" for a user. If you want to learn more, head to
     the
     [reference docs](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-granted-authority)
   - If you want to implement authorities here, see also the handy
     [AuthorityUtils](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/authority/AuthorityUtils.html)
     utility class

Additionally, if you look at the documentation of `AbstractAuthenticationToken`, it is recommended
that subclasses be immutable. Try and make your implementation immutable.

---

<details>

<summary>📖 RobotAuthenticationToken.java</summary>

```java
public class RobotAuthenticationToken extends AbstractAuthenticationToken {

    public RobotAuthenticationToken(Collection<? extends GrantedAuthority> authorities) {
        super(AuthorityUtils.createAuthorityList("ROLE_robot"));
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        throw new RuntimeException("I am immutable!");
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return "Ms Robot 🤖";
    }

}
```

</details>

---

We must then use this implementation in the `RobotAuthenticationFilter`: if the secret is correct,
instantiate that class, and set it in the `SecurityContext`. Those can be created and set using the
`SecurityContextHolder`. You will find a full example in the
[reference docs](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-securitycontextholder).

Update the security context by using a newly created `RobotAuthenticationFilter`.

---

<details>

<summary>📖 RobotAuthenticationFilter.java</summary>

```java
public class RobotAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // ...

        if (!"beep-boop".equals(secret)) {
            // ...
        }

        var newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(new RobotAuthenticationToken());
        SecurityContextHolder.setContext(newContext);

        filterChain.doFilter(request, response);
    }

}
```

</details>

---

Now you should be able to access the private page, with the new authentication mode. Try it with
curl, httpie, postman, etc. Notice that in the private page, you are greeted as "Ms Robot".

With curl:

```shell
curl localhost:8080/private -H "x-robot-secret: beep-boop"
# <!DOCTYPE html>
# <html lang="en">
# <head>
# 	<meta charset="UTF-8">
# 	<title>🔐 Private Page [Spring Sec: The Good Parts]</title>
# 	<link rel="stylesheet" href="css/style.css">
# 	<link rel="icon" href="/favicon.svg" type="image/svg+xml">
# </head>
# <body>
# 	<h1>VIP section 🥳🎉🎊</h1>
# 	<p>Hello, ~[Ms Robot 🤖]~ !</p>
# 	<p>You are on the very exclusive private page.</p>
# 	<p></p>
# 	<form method="post" action="/logout">
# 		<input name="_csrf" type="hidden" value="Gq4yktdpN_Dn_sbr6GDpUcuhUwY9R8HM7ct5k2Ofs4cqzVNNeM0E9-5ZAMLKnaffiU3dZ_nHfj9Zcvnh2vkY8lepirFPq2p5">
# 		<button class="btn" type="submit">Log out</button>
# 	</form>
# </body>
# </html>
```

With httpie:

```shell
http :8080/private x-robot-secret:beep-boop
# ...
```

## Step 4: restoring the other flows

Great: we have fulfilled the request from the Ops team, and we have a robot account but if you have
followed this guide strictly ... we have broken the app! If you try and visit http://localhost:8080,
you will see the error message you chose to display when you detect an invalid `x-robot-secret`.
That's because the browser is not sending an `x-robot-secret`, and the robot flow should only be
trigger in special cases, not the general, browser-based use-case. We should fix this before
deploying to production!

The usual structure of a filter performing authentication is:

1. Chose whether the filter applies or not
2. Try to authenticate
3. Handle the result (success or failure)

We have performed step 2 and 3, but not step 1.

Add a "guard" at the top of the `RobotAuthenticationFilter` that checks whether we are trying to
authenticate the `x-robot-secret` header. If the header is not present, do not even try to
authenticate and go straight to the default behavior of the filter chain.

---

<details>

<summary>📖 RobotAuthenticationFilter.java</summary>

```java
public class RobotAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROBOT_HEADER_NAME = "x-robot-secret";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!Collections.list(request.getHeaderNames()).contains(ROBOT_HEADER_NAME)) {
            filterChain.doFilter(request, response);
            return; // make sure to skip the rest of the filter logic
        }

        // ...
    }

}
```

</details>

---

Now try the full behavior again:

1. `curl localhost:8080/private -H "x-robot-secret: beep-boop"` should succeed
2. `curl localhost:8080/private -H "x-robot-secret: WHAT-IS-THE-SECRET"` should fail
3. Normal, browser-based flows should still function as usual


## Closing out

We have wired a filter to update the security context and authenticate an entity. You now have the
core building blocks for extending Spring Security! There are a lot of subtle details around those
concepts, but the gist of it is those two classes: `Filter` and `Authentication`.

In the next module, we'll look at shortcuts to handle certain types of authentications flows without
having to write a filter, and to overload existing Spring Security authentication flows.

[^1]:
    Don't do that in prod. Please. Use an existing protocol, e.g. OAuth2, or come up with secure
    ways to expose your applications' health. But we're in the lab now, so `#yolo` 🤘
