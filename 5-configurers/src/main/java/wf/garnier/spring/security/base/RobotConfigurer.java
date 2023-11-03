package wf.garnier.spring.security.base;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

public class RobotConfigurer extends AbstractHttpConfigurer<RobotConfigurer, HttpSecurity> {

    @Override
    public void init(HttpSecurity http) {
        http.authenticationProvider(new RobotAuthenticationProvider());
    }

    @Override
    public void configure(HttpSecurity http) {
        var authManager = http.getSharedObject(AuthenticationManager.class);
        var filter = new RobotAuthenticationFilter(authManager);
        http.addFilterBefore(filter, AuthorizationFilter.class);
    }
}
