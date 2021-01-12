package com.mybank;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  @Value("${resource-server.publicKeyUri}")
  private String publicKeyUri;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Fetch public key from Authorization Server
    HttpRequest request = HttpRequest.newBuilder()
      .uri(new URI(publicKeyUri)).build();

    String response = HttpClient.newBuilder()
      .build().send(request, HttpResponse.BodyHandlers.ofString()).body();

    // Load public key
    String keyString = response
      .replaceAll("-----(BEGIN|END) PUBLIC KEY-----", "")
      .replaceAll(System.lineSeparator(), "");

    byte[] keyBytes = Base64.decodeBase64(keyString);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));

    // Configure all endpoints to be authenticated
    // and use this auth filter in the chain
    http.authorizeRequests()
      .anyRequest().authenticated()
      .and()
      .addFilter(new JWTAuthorizationFilter(authenticationManager(), publicKey));
  }
}
