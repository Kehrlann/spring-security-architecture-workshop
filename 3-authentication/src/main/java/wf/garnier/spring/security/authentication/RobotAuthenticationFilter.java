package wf.garnier.spring.security.authentication;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class RobotAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!Collections.list(request.getHeaderNames()).contains("x-robot-secret")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!Objects.equals(request.getHeader("x-robot-secret"), "beep-boop")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setCharacterEncoding("utf-8");
            response.setHeader("content-type", "text/html;charset=utf-8");
            response.getWriter().write("⛔️🤖 You are not Ms Robot!");
            return;
        }

        var newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(new RobotAuthenticationToken());
        SecurityContextHolder.setContext(newContext);

        filterChain.doFilter(request, response);
    }
}
