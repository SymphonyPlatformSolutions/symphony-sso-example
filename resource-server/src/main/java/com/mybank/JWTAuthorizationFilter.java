package com.mybank;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  public static final String HEADER_STRING = "Authorization";
  public static final String TOKEN_PREFIX = "Bearer ";
  public final RSAPublicKey publicKey;

  public JWTAuthorizationFilter(AuthenticationManager authManager, RSAPublicKey publicKey) {
    super(authManager);
    this.publicKey = publicKey;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
    log.info("Authorising");
    String token = req.getHeader(HEADER_STRING);

    try {
      if (token != null && token.startsWith(TOKEN_PREFIX)) {
        DecodedJWT jwt = JWT.require(Algorithm.RSA512(publicKey, null))
          .build()
          .verify(token.replace(TOKEN_PREFIX, ""));

        String subject = jwt.getSubject();
        String audience = jwt.getAudience().get(0);

        // In reality: check that this user exists

        log.info("Authorised: [{}] {}", audience, subject);

        SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(audience, subject, List.of()));
      }
      chain.doFilter(req, res);
    } catch (Exception e) {
      log.error("Authorisation failed");
    }
  }
}
