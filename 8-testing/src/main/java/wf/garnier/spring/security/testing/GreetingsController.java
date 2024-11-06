package wf.garnier.spring.security.testing;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class GreetingsController {

	@GetMapping("/")
	public String publicPage() {
		return "public";
	}

	@GetMapping("/private")
	public String privatePage(Authentication authentication, Model model) {
		model.addAttribute("name", getName(authentication));
		return "private";
	}

	@GetMapping("/admin")
	public String admin(Authentication authentication, Model model) {
		model.addAttribute("name", getName(authentication));
		return "admin";
	}

	private static String getName(Authentication authentication) {
		if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
			return oidcUser.getEmail();
		}
		return authentication.getName();
	}

}
