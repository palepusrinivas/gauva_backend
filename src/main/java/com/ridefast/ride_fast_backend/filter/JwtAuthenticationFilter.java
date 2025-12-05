package com.ridefast.ride_fast_backend.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ridefast.ride_fast_backend.util.JwtTokenHelper;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final UserDetailsService userDetailsService;
  private final JwtTokenHelper jwtTokenHelper;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.startsWith("/swagger-ui") || 
           path.startsWith("/v3/api-docs") || 
           path.startsWith("/swagger-resources") ||
           path.startsWith("/webjars") ||
           path.equals("/home") ||
           path.startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    /*
     * 1. get token from header
     * Token -> Bearer 15544578787fsh78f7hgf7fg857nhjgf74hgf.fhgfh.fhgfnj
     */

    String requestToken = request.getHeader("Authorization");
    String token = null;
    String username = null;
    if (requestToken != null && requestToken.startsWith("Bearer ")) {
      // remove bearer word and space after it
      token = requestToken.substring(7);
      try {
        username = jwtTokenHelper.getUsernameFromToken(token);
      } catch (IllegalArgumentException e) {
        log.debug("Unable to get JWT token: {}", e.getMessage());
      } catch (ExpiredJwtException e) {
        log.debug("JWT token expired");
      } catch (MalformedJwtException e) {
        log.debug("Invalid JWT token");
      }
    } else {
      if (requestToken != null) {
        log.debug("Authorization header present but does not begin with Bearer");
      }
    }

    // once we get the token now we validate it
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = userDetailsService.loadUserByUsername(username);
      if (jwtTokenHelper.validateToken(token, userDetails)) {
        // authentication
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails,
            null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

      } else {
        log.debug("JWT token validation failed");
      }
    } else {
      // Reduce noise: only log at trace/debug if needed
      log.trace("Skipping JWT auth: username null or context already set");
    }

    filterChain.doFilter(request, response);
  }
}
