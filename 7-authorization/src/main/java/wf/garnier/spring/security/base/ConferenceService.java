package wf.garnier.spring.security.base;

import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.List;

@Component
public class ConferenceService {

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
