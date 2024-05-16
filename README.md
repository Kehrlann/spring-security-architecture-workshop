# Spring Security: The Good Parts workshop

This workshop aims to demystify how Spring Security works, show you what concepts it uses under the
hood, and give you keys for fitting your own use-cases.

From the
[Spring Security FAQ](https://docs.spring.io/spring-security/reference/servlet/appendix/faq.html#appendix-faq-start-simple)

> ## I have a complex scenario. What could be wrong?
>
> You need an understanding of the technologies you intend to use before you can successfully build
> applications with them. Security is complicated. Setting up a simple configuration by using a
> login form and some hard-coded users with Spring Security’s namespace is reasonably
> straightforward. Moving to using a backed JDBC database is also easy enough. However, if you try
> to jump straight to a complicated deployment scenario like [this example scenario], you are almost
> certain to be frustrated. There is a big jump in the learning curve required to set up systems
> such as CAS, configure LDAP servers, and install SSL certificates properly. So you need to take
> things one step at a time.

## Pre-requisites

- Basic knowledge of Spring and Spring Boot
  - Familiarity with Spring Security is not expected, but is a plus
- Java 17+ ; consider using [SDKman](https://sdkman.io/) to install new versions of Java
- An HTTP client, such as `curl`, `httpie`, `Postman`, ...
- Docker, for an OpenID (SSO) provider

## Structure

This workshop is composed of multiple modules. Modules are self-directed work, but feel free to
reach out to the instructor for questions!

Between each module, the instructor will debrief about the previous module, and introduce the
following module with a bit of theory, and some new concepts.

Each module has instructions, and a self-contained project, that can be run independently from other
modules.

Each module will have prompts in the various sections. Each prompt will have a solution, marked with
the 📖 emoji. You can click on the line to expand it.

Try and answer questions by looking at the docs first, rather than looking up the solution
immediately!

There are often links to the reference docs or API docs; it's usually a good idea to at least take a
peek. Getting into this habit will help you understand Spring Security (and many other libs) by
yourself, even after the workshop is over.

Lastly - ALL solutions are in the `solutions` branch, but don't peek, that would defeat the whole
purpose of learning.

## Not covered here

Sorry, we won't have the time to cover everything. We could spend a entire week if we went deep on
all things! Notably, we won't cover:

- Any specific security scheme or pattern, other than in passing (e.g. OAuth, SAML, ...)
- Reactive-HTTP Security: it is fairly similar to Servlet. Refer to
  [the docs](https://docs.spring.io/spring-security/reference/reactive/index.html)
- Testing: Testing is important, but I'm confident you'll figure it out by ... following
  [the docs](https://docs.spring.io/spring-security/reference/servlet/test/index.html) of course :)

## Modules

_instructor_ Welcome ; explain the format ; share the repository

1. Adding Spring Security to an existing project
   - _instructor_ 🗒️ mention WebSecurityConfigurerAdapater and lambda-DSL
   - _instructor_ 🗒️ a word on VS Code and IntelliJ
   - _instructor_ 🗒️ a word on IntelliJ's HTTP client
   - _instructor_ 💡 introduce filters and filter chain
2. Implementing your first filter
   - _instructor_ 🗒️ walk through CsrfFilter
   - _instructor_ 🗒️ explain how to check which filters are registered
   - _instructor_ 💡️ introduce authentication and security context
3. Adding custom authentication
   - _instructor_ 🗒️ a word on persisting authentication between requests
   - _instructor_ 💡 introduce use-case, why a filter would be inconvenient
4. An authentication provider
   - _instructor_ 🗒️ debrief auth types (HTTP Basic vs Form POST)
   - _instructor_ 💡️ present configurers, examples: CSRFConfigurer, HttpBasicConfigurer
5. Configurers (this may be optional)
   - _instructor_ 🗒️ debrief reference docs vs configurers
   - _instructor_ 💡️ explain post-processing and delegation
   - _instructor_ 💡️ note: the following requires Docker, otherwise change the use-case
6. Overloading Spring Security behavior
   - _instructor_ 💡️ explain post-processing and delegation
7. Authorization, permissions and access control
   - _instructor_ 💡️ closing thoughts
