// src/main/java/com/example/ClassiFlea/account/MailService.java
package com.example.ClassiFlea.account;

import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MailService {
  private static final Logger log = LoggerFactory.getLogger(MailService.class);

  private final JavaMailSender sender;

  @Value("${spring.mail.username:}")
  private String from;

  @Value("${spring.mail.host:}")
  private String host;

  @Value("${spring.mail.port:0}")
  private int port;

  public MailService(JavaMailSender sender) {
    this.sender = sender;
  }

  public void sendPlain(String to, String subject, String text) {
    sendPlain(to, subject, text, null);
  }

  public void sendPlain(String to, String subject, String text, String replyTo) {
    String safeSubj = truncate(subject, 120);
    try {
      log.info("[MAIL] preparing host={} port={} from={} to={} subject='{}' bytes={}",
          host, port, mask(from), mask(to), safeSubj, (text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length));

      var msg = sender.createMimeMessage();
      var helper = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());
      helper.setFrom(new InternetAddress(from, "ClassiFlea", StandardCharsets.UTF_8.name()));
      helper.setTo(to);
      if (replyTo != null && !replyTo.isBlank()) helper.setReplyTo(replyTo);
      helper.setSubject(subject);
      helper.setText(text, false);
      sender.send(msg);

      log.info("[MAIL] sent ok to={} subject='{}'", mask(to), safeSubj);
    } catch (Exception e) {
      log.error("[MAIL] send failed to={} subject='{}' cause={}", mask(to), safeSubj, e.toString(), e);
      throw new MailSendException("Failed to send email", e);
    }
  }

  private static String truncate(String s, int n) {
    if (s == null) return "";
    return s.length() <= n ? s : s.substring(0, n) + "...";
  }

  private static String mask(String s) {
    if (s == null) return "";
    int at = s.indexOf('@');
    if (at > 1) return s.charAt(0) + "***" + s.substring(at);
    return "***";
  }
}
