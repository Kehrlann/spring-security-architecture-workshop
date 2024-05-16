package wf.garnier.spring.security.base;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
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
	public String privatePage(Model model, Authentication authentication) {
		model.addAttribute("name", getName(authentication));
		return "private";
	}

	private static String getName(Authentication authentication) {
		if (authentication.getPrincipal() instanceof DefaultOidcUser user) {
			return user.getEmail();
		}
		return authentication.getName();
	}

}
