package wf.garnier.spring.security.authorization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class VerbodenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if ("waar".equalsIgnoreCase(request.getHeader("x-verboden"))) {
            // These two lines are required to have emojis in your responses.
            // - Character encoding needs to be set before you write to the response.
            // - Content-Type is for browser-based interactions
            // YES EMOJIS ARE VERY IMPORTANT, THANK YOU VERY MUCH
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("text/plain;charset=utf-8");

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("⛔⛔⛔⛔️ het is verboden");
            response.getWriter().close(); // optional

            // Absolutely make sure you don't call the rest of the filter chain!!
            return;
        }

        filterChain.doFilter(request, response);
    }

}
