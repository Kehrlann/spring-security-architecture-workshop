package wf.garnier.spring.security.postprocessing;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

public class RobotAuthenticationToken extends AbstractAuthenticationToken {

    private final boolean authenticated;

    private final String secret;

    private RobotAuthenticationToken() {
        super(AuthorityUtils.createAuthorityList("ROLE_robot"));
        this.authenticated = true;
        this.secret = null;
    }

    private RobotAuthenticationToken(String secret) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.authenticated = false;
        this.secret = secret;
    }

    public static RobotAuthenticationToken authenticated() {
        return new RobotAuthenticationToken();
    }

    public static RobotAuthenticationToken unauthenticated(String secret) {
        return new RobotAuthenticationToken(secret);
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        throw new RuntimeException("I am immutable!");
    }

    @Override
    public String getCredentials() {
        return secret;
    }

    @Override
    public Object getPrincipal() {
        return "Ms Robot 🤖";
    }

}
