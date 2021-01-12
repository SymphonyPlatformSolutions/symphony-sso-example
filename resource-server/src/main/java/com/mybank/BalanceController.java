package com.mybank;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BalanceController {
  @GetMapping("/api/balance")
  public long getBalance(UsernamePasswordAuthenticationToken token) {
    String grant = token.getCredentials().toString();
    if (grant.equals("/balance")) {
      return (long) (Math.random() * 777);
    }
    else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect grant");
    }
  }
}
