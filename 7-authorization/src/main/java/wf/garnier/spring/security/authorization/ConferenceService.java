package wf.garnier.spring.security.authorization;

import java.util.Collection;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.AuthorizeReturnObject;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.stereotype.Component;

@Component
public class ConferenceService {

	@PreAuthorize("@usernameAuthorizationService.isAuthorized(authentication.name)")
	@AuthorizeReturnObject
	public Collection<Conference> getConferences() {
		// These are all the conferences I have spoken at in 2023 :)
		return List.of(new Conference("VoxxedDays Zürich", "Sihlcity Arena Cinema"),
				new Conference("VoxxedDays Luxembourg", "Casino 2000"),
				new Conference("RivieraDev", "SKEMA Business School"), new Conference("SpringOne", "The Venitian"),
				new Conference("Swiss Cloud Native Day", "Mt Gurten"), new Conference("Devoxx Belgium", "Kinepolis"),
				new Conference("J-Fall", "Pathé Ede"));
	}

	public static class Conference {

		private final String name;

		private final String venue;

		public Conference(String name, String venue) {
			this.name = name;
			this.venue = venue;
		}

		public String getName() {
			return name;
		}

		@PreAuthorize("hasRole('geoguesser')")
		@HandleAuthorizationDenied(handlerClass = NullValueHandler.class)
		public String getVenue() {
			return venue;
		}

	}

}
