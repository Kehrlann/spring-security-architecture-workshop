# Spring Security Workshop: Authorization

Authorization is an important part of Spring Security, which happens after Authentication. It deals
with "permissions", who is allowed to do what: access an endpoint, call a method...

Despite not being the core of this workshop, it's still interesting to understand at least the
basics. It is a fairly wide topic, if you want to learn more, as usual: read the
[reference docs](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html).

Note that Spring Security 6 has introduced a new `AuthorizationManager`, which replaces the
deprecated `AccessDecisionVoter` and `AccessDecisionManager`.

## The use-case

We are going to apply three authorization rules:

1. Only users with the role `admin` can view the `/admin` page. This will showcase basic HTTP
   request security.
2. Only users with an `a` in their name can list conferences. This will showcase method security,
   with a complicated-ish SPeL expression.
3. Only users logged in with OAuth can view the `/oauth` page. This will showcase "modern" HTTP
   request security.

## The starting app

We are starting from the app that was built in module 4, but we introduce a few new pages, notably
`/admin` and `/oauth`. I recommend you do not start from your own app, but rather use what's
provided here.

We also have 4 users:

- `alice`, password: `password`, has role `admin`
- `bob`, password: `password`, has role `admin`
- `carol`, password: `password`, has role `admin`
- `dave`, password: `password`, has role `user`, not admin!

Here we have Dex, with Docker and Docker Compose for SSO login; feel free to remove and put your own
SSO solution back in if you feel that's better. You can always log in in with Dex and
`admin@example.com` / `password`.

## Step 0: verify that the app runs

As a reminder, you can run the app from the command-line:

```bash
./gradlew :7-authorization:bootRun
```

You can also run from your favorite IDE.

Login as `alice`, navigate to `/private`, `/oauth` and `/admin`. They should all work.

Logout, and log back in using SSO. Navigate to the same pages.

## Step 1: simple HTTP security

We want users with the role `admin` to be able to access the `admin` page, and ONLY users with that role.

Take a look at the reference documentation on
[Authorizing an Endpoint](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#authorizing-endpoints),
and update your security configuration accordingly.

---

<details>

<summary>📖 SecurityConfiguration.java</summary>

```java
public class SecurityConfiguration {

    // ...

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> {
                    // ...
                    authorize.requestMatchers("/admin").hasRole("admin");
                    authorize.anyRequest().authenticated();
                })
                // ...
                .build();
    }

    // ...
}
```

</details>

---

Try logging in with `alice`, and go to http://localhost:8080/admin ; it should work. Log out, log
back in with `dave`, navigate back to http://localhost:8080/admin ; you should see a 403 error.

## Step 2: Method security

This is good for securing HTTP endpoints. But you can also secure individual methods down in the
stack.

Notice that the `/admin` page, in the `GreetingsController`, leverages a bean `ConferenceService`.

We have also implemented a simple bean `UsernameAuthorizationService` that, given a name, returns
whether they are authorized or not.

Reading about
[Authorizing with Annotations](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#authorizing-with-annotations)
and specifically
[Authorizing Methods Programmatically](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-programmatic-authorization)
; annotate `ConferenceService#getConferences()` so only users with an `a` in their username are
allowed to call the method. For this, you should leverage the `UsernameAuthorizationService`.

---

<details>

<summary>📖 solution</summary>

ConferenceService.java:

```java

@Component
public class ConferenceService {

    @PreAuthorize("@usernameAuthorizationService.isAuthorized(authentication.name)")
    public Collection<String> getConferences() {
        // ...
    }

}
```

SecurityConfiguration.java:

```java

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // <-- don't forget to enable method security
public class SecurityConfiguration {

    // ...

}
```

</details>

---

Now, try again, login and navigate to http://localhost:8080/admin

- `alice` and `carol` should still be able to see the page
- `dave` should still get a 403 / Forbidden
- `bob`should get a 403 / Access Denied ... with a stack trace.

This shows an interesting dynamic around where you put your security configuration. It's much
clearer to have everything at the endpoint level, either in a security configuration class, or in
each individual controller.

Putting them in deep down in your service code may be surprising to discover. However, it provides
another level of "defense in depth", to make sure core methods have access control. There is no "one
size fits all".

## (Optional) Step 2: don't blow up when bob navigates to `/admin`

Instead, gracefully handle the exception and don't display the conferences on the page.

This has nothing to do with Spring Security. It could make you think of other ways to handle
"securing endpoints" that do not leverage Spring Security, but rather application logic...

On the other hand, if you want to handle things with redirects to another page, you can configure Spring Security.
Take a look
at [Exception Handling](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-exceptiontranslationfilter) 
in the reference docs. You can update your security configuration like so:

---

<details>

<summary>📖 SecurityConfiguration.java</summary>

```java
public class SecurityConfiguration {

    // ...

   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      return http
              // ...
              .exceptionHandling(exceptions -> {
                 exceptions.accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendRedirect("/private");
                 });
              })
              .build();
   }

    // ...
}
```

</details>

## Step 3: "modern" HTTP authorization

We've seen programmatic authorization for methods, but how does that work with HTTP security?

There is a special `.access()` method, which used to support SpEL expressions like method security,
but now takes an `AuthorizationManager`. That authorization manager can be represented as a lambda
`(Supplier<Authentication>, RequestAuthorizationContext) -> AccessDecision`. You have a nice example
in the
[reference docs again](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#_migrating_expressions) -
do NOT use a `WebExpressionAuthorizationManager`, which is meant as a transitional tool between "the
old way" and "the new way".

Check the type of the authentication in your lambda, and only let through users logged in using
`oauth2Login`. Hint: remember the `Authentication` type from module 6? Otherwise use a breakpoint.

---

<details>

<summary>📖 SecurityConfiguration.java</summary>

```java
public class SecurityConfiguration {

    // ...

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> {
                    // ...
                    authorize.requestMatchers("/oauth").access(
                            (authSupplier, context) -> {
                                Authentication authentication = authSupplier.get();
                                return new AuthorizationDecision(
                                        authentication instanceof OAuth2LoginAuthenticationToken);
                            });
                    authorize.anyRequest().authenticated();
                })
                // ...
                .build();
    }

    // ...
}
```

</details>

---

Log in as `alice` and try navigating to http://localhost:8080/oauth . It should yield a 403 error.
Now log out, log back in using SSO, then navigate to http://localhost:8080/oauth ; it should work.

## Closing out

That's all for a basic authorization overview. Obviously those are toy examples, not real-world
use-cases.

There are other utilities, such as Post-Authorization, Pre- and Post-Filtering, but those are very
specific use-cases that I do not encourage using unless you have another choice. I do encourage you
to read the whole
[Authorization section](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)
in the reference docs, there are discussions and nuances of what to use when.
