---
theme: default
class: "text-center"
highlighter: prism
lineNumbers: true
transition: none
# use UnoCSS
css: unocss
aspectRatio: "16/9"
colorSchema: "light"
---

# Spring Security

## The Good Parts™

<br>
<br>

### Daniel Garnier-Moiroux

J-Fall, Ede/Veenendall, 2023-11-08

<!-- 
presenter notes go here
TODO: splash image
-->

---
layout: image-right
image: /daniel-intro.jpg
class: smaller
---

#### Daniel
### Garnier-Moiroux
<br>

Software Engineer

- <logos-spring-icon /> VMWare+Tanzu+Spring
- <logos-twitter /> @Kehrlann
- <logos-mastodon-icon /> @Kehrlann@hachyderm.io
- <logos-firefox /> https://garnier.wf/
- <logos-github-icon /> github.com/Kehrlann/
- <fluent-emoji-flat-envelope-with-arrow /> dgarnier@vmware.com

---

# What we'll talk about

1. Intro, housekeeping
1. Workshop content (theory)
    1. `Filter`: HTTP building block
    1. `Authentication`: domain language
    1. `AuthenticationProvider`: easier authentication
    1. `Configurers`: how things are wired together
    1. Overloading existing behavior
    1. Authorization

---
layout: section
---

# Spring Security

# <br>

---
layout: section
---

# Spring Security

# 😬

---
layout: section
---

# Spring Security

# 🤯

---
layout: section
---

# Spring Security

# 😱🤕😵‍💫

---
layout: section
---

# Spring Security

# ❤️

---
layout: default
---

> ### I have a complex scenario. What could be wrong?
>
> You need an understanding of the technologies you intend to use before you can successfully build
> applications with them. Security is complicated. Setting up a simple configuration by using a
> login form and some hard-coded users with Spring Security’s namespace is reasonably
> straightforward. Moving to using a backed JDBC database is also easy enough. However, if you try
> to jump straight to a complicated deployment scenario like [this example scenario], you are almost
> certain to be frustrated. There is a big jump in the learning curve required to set up systems
> such as CAS, configure LDAP servers, and install SSL certificates properly. So you need to take
> things one step at a time.

---
layout: default
---

# What we won't talk about

- Reactive
    - Focus on Servlet
- Testing
- Not much about Authorization
    - Focus on Authentication

---
layout: default
---

# What you'll need

- Basic knowledge of Spring and Spring Boot
  - Familiarity with Spring Security is not expected, but is a plus
- Java 17+ ; consider using [SDKman](https://sdkman.io/) to install new versions of Java
- An HTTP client, such as `curl`, `httpie`, `Postman`, ...
- Docker, for an OpenID (SSO) provider

---
layout: default
---

# How we'll work

- 7 self-guided modules
- Between each module
    - Debrief
    - Introduce theory for the next module
  
<br>

> It's the first time I give this workshop. Some things may not work. 
>
> Thank you for your understanding 🙇

---
layout: default
---

## Let's get started!

**https://bit.ly/jfall-spring-security**

<img src="/qr-code.png" style="margin: auto; height: 300px;" >

---
layout: section
---

## _Module 1_

# Adding Spring Security

---
layout: default
---

# _Module 1_ debrief

- WebSecurityConfigurerAdapter
- Lambda-DSL
- Secure by default
- IntelliJ HTTP client

---

# Spring Security Filters

```java
public void doFilter(
  HttpServletRequest request, 
  HttpServletResponse response, 
  FilterChain chain
  ) {
    // 1. Before the request proceeds further (e.g. authentication or reject req)
    // ...

    // 2. Invoke the "rest" of the chain
    chain.doFilter(request, response);

    // 3. Once the request has been fully processed (e.g. cleanup)
    // ...
}
```

---
layout: image
image: security-filter-chain.png
---

---
layout: image
image: filter-chain-oop.png
---

---

<img src="filter-chain-call-stack.png" style="height: 500px; margin: auto;" />

---

<img src="filterchain-callstack-2.jpg" />


---
layout: section
---

## _Module 2_

# Writing your first filter


---
layout: default
---

# _Module 2_ debrief

1. Create a `Filter`
    1. Takes HttpServletRequest, HttpServletResponse in
    2. Reads from request, (sometimes) writes to Response
    3. Sometimes does nothing!
2. Register the `Filter` in the `SecurityFilterChain`
    1. _Before_ `AuthorizationFilter.class`
    2. Or any other filter you know.

---

# Discovering Filters

- `DefaultSecurityFilterChain`
- `FilterChainProxy`
- Logs

---
layout: section
---

## A "real" example

`CsrfFilter.java`

---

## "Cross Site Request Forgery"

<img src="csrf-exploit.png" style="height: 400px; margin: auto;">

---

## Protection

<img src="csrf-protection.png" style="height: 400px; margin: auto;">

---
layout: section
---

## A "real" example

Spring Security: `CsrfFilter.java`

---
layout: section
---

# Authentication

Knowing the user...

---

# Authentication objects

##

Spring Security produces `Authentication`. They are used for:
- Authentication (`authn`): _who_ is the user?
- Authorization (`authz`): is the user _allowed to perform_ XYZ?

---

# Vocabulary

- **Authentication**: represents the user. Contains:
  - **Principal**: user "identity" (name, email...)
  - **GrantedAuthorities**: "permissions" (`roles`, ...)

```text
More details in the reference docs:

> Servlet Applications
    > Authentication
        > Architecture
```

---

# Vocabulary (cont')

- **Authentication** also contains:
  - **.isAuthenticated()**: almost always `true`
  - **details**: details about the _request_
  - (Credentials): "password", often `null`

```text
More details in the reference docs:

> Servlet Applications
    > Authentication
        > Architecture
```

---

# For your own apps

##

**DO NOT**

Use `UsernamePasswordAuthenticationToken` everywhere

<br>

**INSTEAD**

Create your own `Authentication` subclasses

---

# Wrapped in a SecurityContext

- The core class is `SecurityContextHolder`
- `public static SecurityContext getContext()`
- Thread-local, not propagated to child threads
- Cleared after requests is 100% processed

Note: there is also a `SecurityContextHolderStrategy`

---

# Remember our filter?

```java
public void doFilter(
  HttpServletRequest request, 
  HttpServletResponse response, 
  FilterChain chain
  ) {
    // 1. Before the request proceeds further (e.g. authentication or reject req)
    // ...

    // 2. Invoke the "rest" of the chain
    chain.doFilter(request, response);

    // 3. Once the request has been fully processed (e.g. cleanup)
    // ...
}
```

---

# More like this

```java
public void doFilter(
  HttpServletRequest request, 
  HttpServletResponse response, 
  FilterChain chain
  ) {
    // 1. Decide whether the filter should be applied

    // 2. Apply filter: authenticate or reject request

    // 3. Invoke the "rest" of the chain
    chain.doFilter(request, response);

    // 4. No cleanup
}
```

---
layout: section
---

## _Module 3_

# Custom authentication

---

# _Module 3_ debrief

##

Some filters produce an `Authentication`

1. Read the request ("convert" to domain object)
2. Authenticate (are the credentials valid?)
3. Save the `Authentication` in the `SecurityContext`
4. Or reject the request when creds invalid

---

# Saving SecurityContext between requests

##

By default, the security context is only valid for the current request.

To re-use your authentication accross requests, use `HttpSessionSecurityContextRepository`.

---
layout: section
---

## `AuthenticationProvider`

For simpler authentication

---

## Authentication

About those `Authentication`s... I may have oversimplified a bit.

🤫

---

<img src="authentication-manager.png" height="400px" style="height: 400px; margin: auto;">

---

<img src="provider-manager.png" height="400px" style="height: 400px; margin: auto;">

---


---
layout: section
---

## _Module 4_

<br>

## `AuthenticationProvider`

---

# _Module 4_ debrief

1. `Authentication`s are a sets of credentials for authentication AND the
output of successful authentication.
2. Transforming credentials into authentication happens in `AuthenticationProviders`
3. That's where you should create your own authentication rules

---
layout: section
---

## _Module 5_

<br>

## `Configurers`

---

# _Module 5_ debrief

- This advanced stuff!
- Configurers vs Reference documentation

---
layout: section
---

## _Module 6_

# Extending Spring Security

---
layout: section
---

## _Module 6_

# Extending Spring Security

---

# _Module 6_ debrief

- Usually all the extension points are available
- But you can roll your own
- Avoid copy-paste at all cost!
    - Extend or delegate instead

---
layout: section
---

# Authorization

---
layout: section
---

## _Module 7_

# Authorization

---

# _Module 7_ debrief

- Prefer authorities to custom authorization
- Use method security parsimoniously
- Be wary of `@PostFilter` and `@PostAuthorize`

---
layout: section
---

# We did it!

# 👏🎉🥳

---
layout: default
---

# References


### **🥺 Feedback please 🥺**

<!-- ouch the hack -->
<!-- https://mobile.devoxx.com/events/dvbe23/talks/2943/details -->
<div style="float:right; margin-right: 50px; text-align: center;">
  <img src="/qr-code-feedback.png" style="margin-bottom: -45px; margin-top: -15px;" >
</div>

<br>

- <logos-twitter /> @Kehrlann
- <logos-mastodon-icon /> @Kehrlann@hachyderm.io
- <logos-firefox /> https://garnier.wf/
- <fluent-emoji-flat-envelope-with-arrow /> dgarnier@vmware.com


---
layout: image
image: /meet-me.jpg
class: end
---

# **Thank you 😊**
