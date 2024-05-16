package wf.garnier.spring.security.authorization;

import java.util.Collection;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ConferenceService {

    @PreAuthorize("@usernameAuthorizationService.isAuthorized(authentication.name)")
    public Collection<String> getConferences() {
        // These are all the conferences I have spoken at in 2023 :)
        return List.of(
                "VoxxedDays Zürich",
                "VoxxedDays Luxembourg",
                "RivieraDev",
                "SpringOne",
                "Swiss Cloud Native Day",
                "Devoxx Belgium",
                "J-Fall");

    }

}
