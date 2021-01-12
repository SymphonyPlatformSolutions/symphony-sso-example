package com.mybank;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.gen.api.model.V4Message;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
public class MessageIdentityService {
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Value("${auth-server.privateKey}")
  private String privateKeyPath;

  @Value("${auth-server.publicKey}")
  private String publicKeyPath;

  private final MessageService messageService;
  private final UserService userService;

  public MessageIdentityService(MessageService messageService, UserService userService) {
    this.messageService = messageService;
    this.userService = userService;
  }

  @GetMapping("/public-key")
  public String getPublicKey() throws Exception {
    log.info("Public key retrieved");
    return Files.readString(Paths.get(publicKeyPath));
  }

  @PostMapping("/token")
  public String getToken(@RequestBody MessageIdentityRequest request) throws Exception {
    V4Message message = messageService.getMessage(request.getMessageId());
    if (message == null) {
      throw new ResponseStatusException(BAD_REQUEST, "No such message");
    }

    List<UserV2> users;
    try {
      users = userService.listUsersByEmails(List.of(request.getUsername()));
    } catch (NullPointerException e) {
      throw new ResponseStatusException(BAD_REQUEST, "No such user");
    }

    if (!message.getUser().getUserId().equals(users.get(0).getId())) {
      throw new ResponseStatusException(BAD_REQUEST, "Message did not originate from user");
    }

    String msgText = message.getMessage()
      .replaceAll("<[^>]*>", "");
    Pattern pattern = Pattern.compile("(?<=((^|\\s)/?))(?:(?!@)[^\\s])+(?=($|\\s))");
    Matcher matcher = pattern.matcher(msgText);

    if (!matcher.find()) {
      throw new ResponseStatusException(BAD_REQUEST, "Unable to determine subject");
    }

    String username = users.get(0).getEmailAddress();
    String subject = matcher.group(0);

    log.info("Generating JWT for user: [{}] and subject: [{}]", username, subject);

    return generateJwt(username, subject);
  }

  private String generateJwt(String username, String subject) throws Exception {
    RSAPrivateKey privateKey = readPrivateKey(privateKeyPath);
    RSAPublicKey publicKey = readPublicKey(publicKeyPath);
    Instant now = Instant.now();

    return JWT.create()
      .withIssuedAt(Date.from(now))
      .withExpiresAt(Date.from(now.plus(5L, ChronoUnit.MINUTES)))
      .withAudience(username)
      .withSubject(subject)
      .sign(Algorithm.RSA512(publicKey, privateKey));
  }

  private Key readKey(String filePath, boolean isPrivate) throws Exception {
    String keyString = Files.readString(Paths.get(filePath))
      .replaceAll("-----(BEGIN|END) (PUBLIC|PRIVATE) KEY-----", "")
      .replaceAll(System.lineSeparator(), "");

    byte[] keyBytes = Base64.decodeBase64(keyString);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    if (isPrivate) {
      return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
    return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
  }

  public RSAPrivateKey readPrivateKey(String filePath) throws Exception {
    return (RSAPrivateKey) readKey(filePath, true);
  }

  public RSAPublicKey readPublicKey(String filePath) throws Exception {
    return (RSAPublicKey) readKey(filePath, false);
  }
}
