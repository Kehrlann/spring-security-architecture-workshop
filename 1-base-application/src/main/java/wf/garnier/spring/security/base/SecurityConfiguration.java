package wf.garnier.spring.security.base;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/").permitAll();
                    authorize.requestMatchers("/css/**").permitAll();
                    authorize.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    authorize.anyRequest().authenticated();
                })
                .formLogin(login -> {
                    login.defaultSuccessUrl("/private");
                })
                .logout(logout -> {
                    logout.logoutSuccessUrl("/");
                })
                .oauth2Login(Customizer.withDefaults())
                .build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        var daniel = User.withDefaultPasswordEncoder()
                .username("daniel")
                .password("password")
                .build();
        var alice = User.withDefaultPasswordEncoder()
                .username("alice")
                .password("password")
                .build();
        return new InMemoryUserDetailsManager(daniel, alice);
    }
}
