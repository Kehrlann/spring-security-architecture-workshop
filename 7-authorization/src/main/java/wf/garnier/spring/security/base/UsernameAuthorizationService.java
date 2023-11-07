package wf.garnier.spring.security.base;

import org.springframework.stereotype.Component;

@Component
public class UsernameAuthorizationService {

    public boolean isAuthorized(String name) {
        return name.contains("a");
    }

}
