package com.mybank;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "BalanceClient", url = "${bot.resource-server-uri}")
public interface BalanceClient {
  @GetMapping("/api/balance")
  String getBalance(@RequestHeader("Authorization") String token);
}
