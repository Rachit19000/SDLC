package com.sdlc.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Extract user information from GitHub
        String githubId = attributes.get("id").toString();
        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");

        log.info("GitHub OAuth2 login successful:");
        log.info("  GitHub ID: {}", githubId);
        log.info("  Username: {}", login);
        log.info("  Name: {}", name);
        log.info("  Email: {}", email);

        // Generate mock token (in production, use JWT)
        String token = "oauth_token_" + githubId + "_" + System.currentTimeMillis();

        // Store user info in session or create JWT token
        request.getSession().setAttribute("user_id", "user_github_" + githubId);
        request.getSession().setAttribute("user_email", email != null ? email : login + "@github.local");
        request.getSession().setAttribute("user_name", name != null ? name : login);
        request.getSession().setAttribute("auth_token", token);
        request.getSession().setAttribute("avatar_url", avatarUrl);

        // Redirect to frontend dashboard with token
        String redirectUrl = "http://localhost:3000/dashboard?token=" + token + 
                           "&user=" + login + 
                           "&email=" + (email != null ? email : "");
        
        log.info("Redirecting to: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
