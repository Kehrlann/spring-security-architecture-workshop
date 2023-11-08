# Spring Security Workshop: Writing your first filter

This module will be the first where we interact with some Spring Security architecture concepts.

You will build your first `Filter` implementation, register it in the filter chain.

## The app

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

## Step 1: create and register a filter

We will implement an instance of
[jakarta.servlet.Filter](https://jakarta.ee/specifications/platform/9/apidocs/jakarta/servlet/filter).

There is a guide in the
[Spring Security docs](https://docs.spring.io/spring-security/reference/servlet/architecture.html#adding-custom-filter),
which is helpful reference material, but for once I recommend you follow what's in this README
instead.

Jakarta is a very low-level API, we will prefer using Spring whenever possible. The basic Filter
class for Spring is called
[GenericFilterBean](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/GenericFilterBean.html),
in the words found in the JavaDoc above:

> A handy superclass for any type of filter.

Your task is to implement a Filter that logs a simple message in the console when it is invoked, and
then lets the request proceed as usual. Use a Spring-based filter class as the base class. You can
call this Filter `VerbodenFilter`, we will be using it to mark certain requests as "Forbidden".

Notice, in the JavaDoc for `GenericFilterBean` above, that there are "Direct Known Subclasses".
Maybe you should use one of those? (Hint: use the most "popular" subtype, that is, the one with the
most subclasses.). Don't forget to call back to the FilterChain!

---

<details>

<summary>📖 VerbodenFilter.java</summary>

```java
public class VerbodenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        System.out.println("~~~~> 👋 Hello from VerbodenFilter!");
        filterChain.doFilter(request, response);
    }

}
```

</details>

---

Try to log in, the app should work just as before. Notice that just creating this filter does not do
anything, in fact it doesn't even log to the console.

That's because we need to add it to the `SecurityFilterChain`. Go back to your Security
configuration, and add the filter. Remember, filters are ordered, so you need to choose _where_ in
the chain you want to add such filter. A good place to put it is before the `AuthorizationFilter`
filter. We will discuss why after this module.

---

<details>

<summary>📖 SecurityConfiguration.java</summary>

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ...
                .addFilterBefore(new VerbodenFilter(), AuthorizationFilter.class)
                .build();
    }

    // ...
}
```

</details>

---

Load the app in your browser, and make sure you get a log. You will likely see multiple entries -
that's because a page of the app requests the CSS file as well as the favicon.

## Step 2: adding security logic to the filter

We have a functioning filter, let's turn it into something useful!

We now want to check whether a request is sent with the header `x-verboden` set to `waar` (or
`x-forbidden` set to `true` if you prefer English!). This is not a real use-case, but you could
imagine a filter that checks the `User-Agent` header and only allows some browsers to view your app.

Using your IDE, try to find how you can get the value for that header in the request.

Once you have the value, when it is set to `waar` / `true`, we want to reject the request. To reject
a request is to send a response that says "nope that request you just sent me is not allowed". What
you need to do is:

1. Set the status code of the response - here, let's use `HTTP 403 - Forbidden`
2. Possibly, write a message in the body of the response to tell the user why their request is
   rejected. Optionally, close the response body, so that no other code path can write to it
   - Note: you want to get java `Writer` from the response, and call `write` or `print` on it
3. Make sure that the rest of the filter chain is not executed - otherwise you'll have problems,
   some components may want to write to the response.

---

<details>

<summary>📖 VerbodenFilter.java</summary>

```java
public class VerbodenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if ("waar".equalsIgnoreCase(request.getHeader("x-verboden"))) {
            // These two lines are required to have emojis in your responses.
            // - Character encoding needs to be set before you write to the response.
            // - Content-Type is for browser-based interactions
            // YES EMOJIS ARE VERY IMPORTANT, THANK YOU VERY MUCH
            response.setCharacterEncoding(StandardCharset.UTF_8.name());
            response.setContentType("text/plain;charset=utf-8");

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("⛔⛔⛔⛔️ het is verboden");
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

Visit the page in your browser. It should still work as before. If it doesn't, make sure you handle
the case where the header is absent: your browser is not sending this `x-verboden` header we
invented!

Now let's check that the filter does work, that we get a 403 and a response body.

With curl:

```shell
curl -v localhost:8080 -H "x-verboden: waar"
# *   Trying 127.0.0.1:8080...
# * Connected to localhost (127.0.0.1) port 8080 (#0)
# > GET / HTTP/1.1
# > Host: localhost:8080
# > User-Agent: curl/8.1.2
# > Accept: */*
# > x-verboden: waar 💡
# >
# < HTTP/1.1 403 💡
# < X-Content-Type-Options: nosniff
# < X-XSS-Protection: 0
# < Cache-Control: no-cache, no-store, max-age=0, must-revalidate
# < Pragma: no-cache
# < Expires: 0
# < X-Frame-Options: DENY
# < Content-Type: text/plain;charset=utf-8
# < Content-Length: 31
# < Date: Tue, 24 Oct 2023 20:34:39 GMT
# <
# * Connection #0 to host localhost left intact
# ⛔⛔⛔⛔️ het is verboden 💡
```

With httpie:

```shell
http :8080 x-verboden:waar
# HTTP/1.1 403
# Cache-Control: no-cache, no-store, max-age=0, must-revalidate
# Connection: keep-alive
# Content-Length: 31
# Content-Type: text/plain;charset=utf-8
# Date: Tue, 24 Oct 2023 20:36:49 GMT
# Expires: 0
# Keep-Alive: timeout=60
# Pragma: no-cache
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# X-XSS-Protection: 0
#
# ⛔⛔⛔⛔️ het is verboden
```

## Optional

While the rest of the group is catching up, you can check out other filter implementations if you
are curious. We will look at the `CsrfFilter` together.

## Closing out

We have implemented our first filter! The first step to interacting with requests just like Spring
Security does it.

In the next module, we'll look at how we can leverage filters to _authenticate_ an incoming request,
that is, identifying who the "entity" that created this request is, and validating this identity
with credentials.
