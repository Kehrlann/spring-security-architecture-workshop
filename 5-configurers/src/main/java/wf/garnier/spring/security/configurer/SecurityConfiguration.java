package wf.garnier.spring.security.configurer;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/").permitAll();
                    authorize.requestMatchers("/css/**").permitAll();
                    authorize.requestMatchers("/error").permitAll();
                    authorize.requestMatchers("/favicon.svg").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .formLogin(form -> {
                    form.defaultSuccessUrl("/private");
                })
                .oauth2Login(oidc -> {
                    oidc.defaultSuccessUrl("/private");
                })
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(new ForbiddenFilter(), AuthorizationFilter.class)
                .addFilterBefore(new RobotAuthenticationFilter(), AuthorizationFilter.class)
                .authenticationProvider(new DanielAuthenticationProvider())
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
