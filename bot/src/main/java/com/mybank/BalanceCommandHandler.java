package com.mybank;

import com.symphony.bdk.core.activity.command.CommandContext;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.spring.annotation.Slash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BalanceCommandHandler {
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final MessageService messageService;
  private final MessageIdentityClient messageIdentityClient;
  private final BalanceClient balanceClient;

  public BalanceCommandHandler(
    MessageService messageService,
    MessageIdentityClient messageIdentityClient,
    BalanceClient balanceClient
  ) {
    this.messageService = messageService;
    this.messageIdentityClient = messageIdentityClient;
    this.balanceClient = balanceClient;
  }

  @Slash(value = "/balance", mentionBot = false)
  public void showBalance(CommandContext context) {
    log.info("Balance requested");
    String response;
    String messageId = context.getMessageId();
    String username = context.getInitiator().getUser().getEmail();

    try {
      // Get token from auth server
      MessageIdentityRequest request = new MessageIdentityRequest(messageId, username);
      String token = messageIdentityClient.getToken(request);

      // Fetch data from resource server
      String authToken = "Bearer " + token;
      String balance = balanceClient.getBalance(authToken);
      response = "Your balance is: $" + balance;
    } catch (Exception e) {
      response = "You are unauthorised to use this feature";
    }

    // Respond to user
    messageService.send(context.getStreamId(), response);
  }
}
