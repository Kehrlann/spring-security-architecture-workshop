package wf.garnier.spring.security.testing;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			Optional<ClientRegistrationRepository> clientRegistrationRepository) throws Exception {
		var filterChainBuilder = http.authorizeHttpRequests(authorize -> {
			authorize.requestMatchers("/", "/css/**", "/error", "/favicon.svg", "/favicon.ico").permitAll();
			authorize.requestMatchers("/admin").hasRole("admin");
			authorize.anyRequest().authenticated();
		}).formLogin(form -> {
			form.defaultSuccessUrl("/private");
		})
			.httpBasic(Customizer.withDefaults())
			.addFilterBefore(new ForbiddenFilter(), AuthorizationFilter.class)
			.addFilterBefore(new RobotAuthenticationFilter(), AuthorizationFilter.class)
			.authenticationProvider(new DanielAuthenticationProvider())
			.exceptionHandling(exceptions -> {
				exceptions.accessDeniedHandler((request, response, accessDeniedException) -> {
					if (accessDeniedException instanceof AuthorizationDeniedException) {
						response.sendRedirect("/private?denied");
						return;
					}
					response.sendRedirect("/login?error");
				});
			});

		if (clientRegistrationRepository.isPresent()) {
			filterChainBuilder.oauth2Login(oidc -> {
				oidc.userInfoEndpoint(userInfo -> userInfo.oidcUserService(new DomainAwareOidcUserService()));
				oidc.defaultSuccessUrl("/private");
			});
		}

		return filterChainBuilder.build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		var userFactory = User.withDefaultPasswordEncoder();
		//@formatter:off
        var alice = userFactory.username("alice")
                .password("password")
                .roles("user", "admin")
                .build();
        var bob = userFactory.username("bob")
                .password("password")
                .roles("user")
                .build();
        //@formatter:on
		return new InMemoryUserDetailsManager(alice, bob);
	}

}
