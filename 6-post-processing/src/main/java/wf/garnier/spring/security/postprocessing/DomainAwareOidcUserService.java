package wf.garnier.spring.security.postprocessing;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class DomainAwareOidcUserService extends OidcUserService {

    private String AUTHORIZED_DOMAIN = "corp.example.com";

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        var oidcUser = super.loadUser(userRequest);
        var domain = oidcUser.getEmail().split("@")[1];
        if (!domain.equals(AUTHORIZED_DOMAIN)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_domain"),
                    "Cannot log in because email has domain [@%s]. Only emails with domain [%s] are accepted."
                            .formatted(domain, AUTHORIZED_DOMAIN));
        }
        return oidcUser;
    }
}