# Spring Security Workshop: Extending Spring-Security code

In the previous module, we have implemented a configurer, just like Spring Security does. That's
great for adding our own code to Spring Security. But what if we want to change a default behavior
in a way that is not supported out of the box?

Some Spring Security configuration you can change through an API, e.g. change the URL of the login
page. But some other use-cases are too custom, so you need to add your own Java code. To avoid
re-implementing what Spring Security already provides and just sprinkling behavior on top of what it
gives you, there are two main ways:

1. Creating subclasses of existing classes
2. Delegation through composition

Subclassing is sometimes impossible because some Spring Security classes are still final, e.g.
`AuthorizedClientServiceOAuth2AuthorizedClientManager`. Another problem with subclasses is that you
would have to think about all the dependencies of those classes and figure out how they should be
set up. For example, the authentication provider used for username/password login is
`DaoAuthenticationProvider`, which extends `AbstractUserDetailsAuthenticationProvider`, it has a
total of 11 private fields. That's a lot to think about!

Much easier is to do composition - you create a class that implements the correct interface, grab
whatever Spring Security has wired in for you, and do pre or post-processing in addition to what the
base class does. For example, with our `DaoAuthenticationProvider` above:

```java
public class CustomDaoAuthenticationProvider implements AuthenticationProvider {

    private final DaoAuthenticationProvider delegate;

    public CustomDaoAuthenticationProvider(DaoAuthenticationProvider delegate) {
        // This "delegate" is injected by ways we will discover in this module
        this.delegate = delegate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // Custom pre-processing
        var preProcessedAuthRequest = preProcess(authentication);

        // Call the delegate that was configured by Spring Security
        var authResult = this.delegate.authenticate(preProcessedAuthRequest);

        // Custom post-processing
        var postProcessedAuthResult = postProcess(authResult);

        return postProcessedAuthResult;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return this.delegate.supports(authentication);
    }

}
```

You could imagine that you do some pre-processing for rate-limiting, or some post-processing for
checking people logging in against their work schedule.

## The use-case

We have an upstream Identity Provider, but people can log in with multiple domains. We want to allow
log-in only from people with `@corp.example.com` e-mail addresses, but not other domains.

## The starting app

We are starting from the app that was built in the previous module. In this particular use-case, I
recommend you use dex with `docker-compose`, as I have pre-loaded users with correct domains.

So you need Docker and docker-compose.

## Step 0: verify that the app runs

As a reminder, you can run the app from the command-line:

```bash
./gradlew :6-post-processing:bootRun
```

You can also run from your favorite IDE.

We are going to focus on logging in with the Dex identity provider, not all the other features we
have implemented.

When you try to log in with Dex, the password for all users is `password`. You have the following 3
users:

- `admin@example.com`
- `alice@corp.example.com`
- `bob@example.com`

## Step 1: finding the extension point

We've seen configurers in the previous module. Using your knowledge, try to find what classes are
involved when logging in with Dex. Hint: we're using OpenID Connect to log in, not raw OAuth2 login.
The short name for OpenID Connect is `oidc`.

You could look through the codes, or take a look at the Spring Security reference docs, in
[OAuth2 Login Advanced configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html).

The solutions below will show you how to find the extension classes by looking at the Spring Security source code. Try
and find by yourself before looking at the solutions, or at the next section. It is ok to look for a bit, and then open
Step 1, which will give you the first "step" or "hint" at finding the extension point.

<details>
<summary>📖 Step 1</summary>

Open and read through the `OAuth2LoginConfigurer`. You'll find that's what is used when you do
`.oauth2Login()`.

</details>

<details>
<summary>📖 Step 2</summary>

Look for `AuthenticationProvider` classes, as this is where authentication happens.

</details>

<details>
<summary>📖 Step 3</summary>

You'll find two `AuthenticationProvider`s:

- `OAuth2LoginAuthenticationProvider`
- `OidcAuthorizationCodeAuthenticationProvider`

Notice how they are customized with calling `.setXXX()` methods - you don't want to understand that
logic, you trust Spring Security to do The Right Thing™.

As mentioned above, you want to take a closer look at `OidcAuthorizationCodeAuthenticationProvider`,
as that the auth provider that deals with OpenID Connect.

</details>

<details>
<summary>📖 The real implementation</summary>

Read the `OidcAuthorizationCodeAuthenticationProvider` javadoc carefully. You should find that loading the user and its
attributes, including the e-mail, is done through an `OidcUserService`.

</details>

## Step 2: Overriding behavior

You should have found that we could do what we want by overriding the `OidcUserService` - for this
use-case, Spring Security already has the extension point for this!

But first, we will pretend the configurer does not have a way to override this user-service.

Implement a `CustomOidcAuthenticationProvider`, using the delegate pattern shown in the
introduction, wrapping the `OidcAuthorizationCodeAuthenticationProvider`. Notice which type of
`Authentication` this provider returns, and find out how to find the e-mail. Upon successful
authentication, it checks the user's e-mail and throws an `LockedException` when the domain does not
match `@corp.example.com`, containing a nice message. It is not the most descriptive exception - you may want to
implement your own `AuthenticationException`, and call it `InvalidEmailDomainException` or something similar.

---

<details>

<summary>📖 CustomOidcAuhtenticationProvider.java</summary>

```java
public class CustomOidcAuthenticationProvider implements AuthenticationProvider {

    private final OidcAuthorizationCodeAuthenticationProvider delegate;
    private String AUTHORIZED_DOMAIN = "corp.example.com";

    public CustomOidcAuthenticationProvider(OidcAuthorizationCodeAuthenticationProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2LoginAuthenticationToken authResult = (OAuth2LoginAuthenticationToken) delegate
                .authenticate(authentication);

        var user = ((OidcUser) authResult.getPrincipal());
        var domain = user.getEmail().split("@")[1];
        if (!domain.equals(AUTHORIZED_DOMAIN)) {
            throw new LockedException(
                    "Cannot log in because email has domain [@%s]. Only emails with domain [%s] are accepted."
                            .formatted(domain, AUTHORIZED_DOMAIN));
        }

        return authResult;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}
```

</details>

---

Note that here, we could have used subclassing, but then we would have had to known how to wire the
dependencies in.

Here, we're not doing anything with the dependencies, so we are happy with whatever defaults Spring
Security wired in for us, and thus benefit from wrapping the existing instance.

## Step 3: Wire in the delegate in your `AuthenticationProvider`

Take a step back, look at how the `OidcAuthorizationCodeAuthenticationProvider` is registered in the
filter chain. Notice that right before it is added in the chain, the configurer calls `postProcess`
on it. This is where we can grab the Spring-Security configured auth-provider. In the
`oauth2Login(...)` customizer, register a post-processor that grabs
`OidcAuthorizationCodeAuthenticationProvider` and returns our own instead.

---

<details>

<summary>📖 SecurityConfiguration.java</summary>

```java
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // ...
        return http
                // ...
                .oauth2Login(oidc -> {
                    oidc.defaultSuccessUrl("/private");
                    oidc.withObjectPostProcessor(
                            // Note: this is an anonymous implementation, but you could
                            // make a real class out of it.
                            new ObjectPostProcessor<AuthenticationProvider>() {
                                @Override
                                public <O extends AuthenticationProvider> O postProcess(O object) {
                                    if (object instanceof OidcAuthorizationCodeAuthenticationProvider oidcProvider) {
                                        return (O) new CustomOidcAuthenticationProvider(oidcProvider);
                                    }
                                    return object;
                                }

                            });
                })
                // ...
                .build();
    }

    // ...

}
```

</details>

---

Now try to log in with Dex, using:

- `alice@corp.example.com`
- `bob@example.com`

Only Alice should be able to log in.

This is not the "right" way to implement this feature, at least that's not what I would recommend.
However, this may come handy when there are missing extension points, or when your use-case is too
particular to fit into Spring Security's model.

## (Optional) Step 4: Doing it "right"

As we've seen in step 1, what we should really do is have our own `OidcUserService`.

Remove the code you have introduced above, and create a subclass of `OicUserService` that implements
the desired behavior. You may also achieve the same result by using composition.

Then inject it in the right place in the `oauth2Login` configuration. There are hints
[in the docs](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html).

---

<details>

<summary>📖 solution</summary>

DomainAwareOidcUserService.java:

```java
public class DomainAwareOidcUserService extends OidcUserService {

    private String AUTHORIZED_DOMAIN = "corp.example.com";

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        var oidcUser = super.loadUser(userRequest);
        var domain = oidcUser.getEmail().split("@")[1];
        if (!domain.equals(AUTHORIZED_DOMAIN)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_domain"),
                    "Cannot log in because email has domain [@%s]. Only emails with domain [%s] are accepted."
                            .formatted(domain, AUTHORIZED_DOMAIN));
        }
        return oidcUser;
    }
}
```

SecurityConfiguration.java

```java
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // ...
        return http
                // ...
                .oauth2Login(oidc -> {
                    oidc.defaultSuccessUrl("/private");
                    oidc.userInfoEndpoint(ui -> {
                        ui.oidcUserService(new DomainAwareOidcUserService());
                    });
                })
                // ...
                .build();
    }
}
```

</details>

---

Try the same as the above, logging in with:

- `alice@corp.example.com` (should work)
- `bob@example.com` (should fail)

## Closing out

You now have the basic principles for extending Spring Security's behavior, either through exposed
configuration methods, or through object post-processing and composition.

Armed with all of this knowledge, you should be able to configure most of Spring Security!
