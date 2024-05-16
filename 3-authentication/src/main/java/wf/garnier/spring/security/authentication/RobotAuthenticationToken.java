package wf.garnier.spring.security.authentication;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

class RobotAuthenticationToken extends AbstractAuthenticationToken {
    public RobotAuthenticationToken() {
        super(AuthorityUtils.NO_AUTHORITIES);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return "🤖 Ms Robot";
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        throw new RuntimeException("🎶 Can't touch this");
    }
}
