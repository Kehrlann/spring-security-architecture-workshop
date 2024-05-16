package wf.garnier.spring.security.authenticationprovider;

import java.util.Objects;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class DanielAuthenticationProvider implements AuthenticationProvider {
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (Objects.equals(authentication.getName(), "daniel")) {
            var daniel = User.withUsername("daniel")
                    .password("~~ignored~~")
                    .roles("admin")
                    .build();
            return UsernamePasswordAuthenticationToken.authenticated(
                    daniel,
                    null,
                    daniel.getAuthorities()
            );
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
