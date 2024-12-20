package wf.garnier.spring.security.testing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RobotAuthenticationFilter extends OncePerRequestFilter {

	private static final String ROBOT_HEADER_NAME = "x-robot-secret";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!Collections.list(request.getHeaderNames()).contains(ROBOT_HEADER_NAME)) {
			filterChain.doFilter(request, response);
			return; // make sure to skip the rest of the filter logic
		}

		var secret = request.getHeader(ROBOT_HEADER_NAME);

		if (!"beep-boop".equals(secret)) {
			// These two lines are required to have emojis in your responses.
			// See ForbiddenFilter for more information.
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType("text/plain;charset=utf-8");

			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.getWriter().write("🤖⛔️ you are not Ms Robot");
			response.getWriter().close(); // optional

			// Absolutely make sure you don't call the rest of the filter chain!!
			return;
		}
		var newContext = SecurityContextHolder.createEmptyContext();
		newContext.setAuthentication(new RobotAuthenticationToken());
		SecurityContextHolder.setContext(newContext);

		filterChain.doFilter(request, response);
	}

}
