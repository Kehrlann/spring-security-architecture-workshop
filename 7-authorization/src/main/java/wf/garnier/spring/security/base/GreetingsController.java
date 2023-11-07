package wf.garnier.spring.security.base;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class GreetingsController {

	private final ConferenceService conferenceController;

	public GreetingsController(ConferenceService conferenceController) {
		this.conferenceController = conferenceController;
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
		model.addAttribute("conferences", conferenceController.getConferences());
		return "admin";
	}

	private static String getName(Authentication authentication) {
		if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
			return oidcUser.getEmail();
		}
		return authentication.getName();
	}

}
