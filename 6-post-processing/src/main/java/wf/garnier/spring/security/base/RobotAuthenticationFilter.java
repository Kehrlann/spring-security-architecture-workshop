package wf.garnier.spring.security.base;

import java.io.IOException;
import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.jose.util.StandardCharset;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RobotAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROBOT_HEADER_NAME = "x-robot-secret";

    private final AuthenticationManager authenticationManager;

    public RobotAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!Collections.list(request.getHeaderNames()).contains(ROBOT_HEADER_NAME)) {
            filterChain.doFilter(request, response);
            return; // make sure to skip the rest of the filter logic
        }

        var secret = request.getHeader(ROBOT_HEADER_NAME);
        var authRequest = RobotAuthenticationToken.unauthenticated(secret);

        try {
            var authentication = authenticationManager.authenticate(authRequest);
            var newContext = SecurityContextHolder.createEmptyContext();
            newContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(newContext);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            // These two lines are required to have emojis in your responses.
            // See VerbodenFilter for more information.
            response.setCharacterEncoding(StandardCharset.UTF_8.name());
            response.setContentType("text/plain;charset=utf-8");

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write(exception.getMessage());
            response.getWriter().close(); // optional

            // We're not calling into the rest of the filter chain here
        }

    }

}
