package com.ridefast.ride_fast_backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String cid = UUID.randomUUID().toString().substring(0, 8);
    long start = System.currentTimeMillis();
    MDC.put("cid", cid);
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String q = request.getQueryString();
    String path = q == null ? uri : uri + "?" + q;
    String ip = request.getRemoteAddr();
    String user = currentPrincipal();
    try {
      log.info("REQ {} {} ip={} user={}", method, path, ip, user);
      filterChain.doFilter(request, response);
    } finally {
      long dur = System.currentTimeMillis() - start;
      int status = response.getStatus();
      log.info("RES {} {} status={} durMs={} user={}", method, path, status, dur, user);
      MDC.remove("cid");
    }
  }

  private String currentPrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return "anonymous";
    return auth.getName();
  }
}
