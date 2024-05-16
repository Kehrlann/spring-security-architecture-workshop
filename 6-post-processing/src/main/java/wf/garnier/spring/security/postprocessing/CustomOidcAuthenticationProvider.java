package wf.garnier.spring.security.postprocessing;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class CustomOidcAuthenticationProvider implements AuthenticationProvider {

    private final OidcAuthorizationCodeAuthenticationProvider delegate;
    private String AUTHORIZED_DOMAIN = "corp.example.com";

    public CustomOidcAuthenticationProvider(OidcAuthorizationCodeAuthenticationProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2LoginAuthenticationToken authResult = (OAuth2LoginAuthenticationToken) delegate
                .authenticate(authentication);

        var user = ((OidcUser) authResult.getPrincipal());
        var domain = user.getEmail().split("@")[1];
        if (!domain.equals(AUTHORIZED_DOMAIN)) {
            throw new LockedException(
                    "Cannot log in because email has domain [@%s]. Only emails with domain [%s] are accepted."
                            .formatted(domain, AUTHORIZED_DOMAIN));
        }

        return authResult;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}