package wf.garnier.spring.security.configurer;

import java.util.Objects;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

class RobotAuthenticationProvider implements AuthenticationProvider {
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var authrequest = (RobotAuthenticationToken) authentication;
        if ("beep-boop".equals(authrequest.getCredentials())) {
            return RobotAuthenticationToken.authenticated();
        }
        throw new BadCredentialsException("🤖⛔️ you are not Ms Robot");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RobotAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
