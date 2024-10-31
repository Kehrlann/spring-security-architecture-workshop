package wf.garnier.spring.security.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class GreetingsController {

	private final ConferenceService conferenceService;

	public GreetingsController(ConferenceService conferenceService) {
		this.conferenceService = conferenceService;
	}

	@GetMapping("/")
	public String publicPage() {
		return "public";
	}

	@GetMapping("/private")
	public String privatePage(Authentication authentication, Model model) {
		model.addAttribute("name", getName(authentication));
		return "private";
	}

	@GetMapping("/oauth")
	public String oauth(Authentication authentication, Model model) {
		model.addAttribute("name", getName(authentication));
		return "oauth";
	}

	@GetMapping("/admin")
	public String admin(Authentication authentication, Model model) {
		model.addAttribute("name", getName(authentication));
		model.addAttribute("conferences", conferenceService.getConferences().stream().map(this::formatConference));
		return "admin";
	}

	private String formatConference(ConferenceService.Conference conference) {
		if (conference.getVenue() == null) {
			return conference.getName();
		}

		return "%s (%s)".formatted(conference.getName(), conference.getVenue());
	}

	private static String getName(Authentication authentication) {
		if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
			return oidcUser.getEmail();
		}
		return authentication.getName();
	}

}
