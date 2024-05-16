package wf.garnier.spring.security.configurer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RobotAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROBOT_HEADER_NAME = "x-robot-secret";

    private final AuthenticationManager authenticationManager;

    public RobotAuthenticationFilter(AuthenticationManager authManager) {
        this.authenticationManager = authManager;
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
        var authRequest = RobotAuthenticationToken.authenticationRequest(secret);
        try {
            var auth = authenticationManager.authenticate(authRequest);
            var newContext = SecurityContextHolder.createEmptyContext();
            newContext.setAuthentication(auth);
            SecurityContextHolder.setContext(newContext);
        } catch (AuthenticationException e) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("text/plain;charset=utf-8");

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write(e.getMessage());
            response.getWriter().close();

            return;
        }

        filterChain.doFilter(request, response);
    }

}
