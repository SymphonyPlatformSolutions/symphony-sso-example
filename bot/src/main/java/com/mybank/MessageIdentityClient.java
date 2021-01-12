package com.mybank;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "MessageIdentityClient", url = "${bot.auth-server-uri}")
public interface MessageIdentityClient {
  @PostMapping("/token")
  String getToken(MessageIdentityRequest request);
}
