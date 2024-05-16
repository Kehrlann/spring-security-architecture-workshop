package wf.garnier.spring.security.filter;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Optional<ClientRegistrationRepository> clientRegistrationRepository
    ) throws Exception {
        var filterChainBuilder = http
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/").permitAll();
                    authorize.requestMatchers("/css/**").permitAll();
                    authorize.requestMatchers("/error").permitAll();
                    authorize.requestMatchers("/favicon.svg").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .formLogin(form -> {
                    form.defaultSuccessUrl("/private");
                });
        // Conditionally enable OAuth2 login
        // When spring.security.oauth2.client.registration... properties are set, such as in the "docker" profile,
        // Spring Boot creates a ClientRegistrationRepository for you. In that case, we'll add OAuth2 login.
        if (clientRegistrationRepository.isPresent()) {
            filterChainBuilder
                    .oauth2Login(oidc -> {
                        oidc.defaultSuccessUrl("/private");
                    });
        }
        return filterChainBuilder
                .addFilterBefore(new ForbiddenFilter(), AuthorizationFilter.class)
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
