package wf.garnier.spring.security.postprocessing;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class RobotAuthenticationProvider implements AuthenticationProvider {

    private final String secret = "beep-boop";

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var authrequest = (RobotAuthenticationToken) authentication;
        if (secret.equals(authrequest.getCredentials())) {
            return RobotAuthenticationToken.authenticated();
        }
        throw new BadCredentialsException("🤖⛔️ you are not Ms Robot");

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RobotAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
