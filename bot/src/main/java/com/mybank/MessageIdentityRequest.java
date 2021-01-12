package com.mybank;

public class MessageIdentityRequest {
  private String messageId;
  private String username;

  public MessageIdentityRequest() {}

  public MessageIdentityRequest(String messageId, String username) {
    this.messageId = messageId;
    this.username = username;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
