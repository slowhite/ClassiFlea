// src/main/java/com/example/ClassiFlea/account/AccountController.java
package com.example.ClassiFlea.account;

import com.example.ClassiFlea.user.User;
import com.example.ClassiFlea.user.UserRepository;
import com.example.ClassiFlea.profile.Profile;
import com.example.ClassiFlea.profile.ProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountController {
  private static final String PURPOSE_REGISTER = "REGISTER";

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final ProfileRepository profiles;
  private final EmailCodeRepository codes;   // 验证码仓库
  private final MailService mail;            // 发邮件服务

  public AccountController(UserRepository u, PasswordEncoder e, ProfileRepository p,
                           EmailCodeRepository c, MailService m) {
    this.users = u; this.encoder = e; this.profiles = p;
    this.codes = c; this.mail = m;
  }

  @PostMapping("/password")
  public ResponseEntity<?> changePwd(Principal me, @RequestBody Map<String,String> body){
    if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    var u = users.findByEmail(me.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var oldPwd = String.valueOf(body.getOrDefault("oldPassword",""));
    var newPwd = String.valueOf(body.getOrDefault("newPassword",""));
    if (!encoder.matches(oldPwd, u.getPasswordHash()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "旧密码不正确");
    if (newPwd == null || newPwd.length() < 8)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码至少8位");
    u.setPasswordHash(encoder.encode(newPwd));
    users.save(u);
    return ResponseEntity.noContent().build();
  }

  // 发送注册验证码（60 秒限频，10 分钟有效）
  @PostMapping("/verification-code")
  public ResponseEntity<?> sendCode(@RequestBody Map<String,String> body){
    String email = String.valueOf(body.getOrDefault("email","")).trim().toLowerCase();
    if (email.isEmpty() || !email.contains("@"))
      return ResponseEntity.badRequest().body(Map.of("error","邮箱不合法"));
    if (users.findByEmail(email).isPresent())
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","该邮箱已被注册"));

    var latest = codes.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, PURPOSE_REGISTER);
    if (latest.isPresent() && latest.get().getCreatedAt().isAfter(Instant.now().minusSeconds(60))) {
      return ResponseEntity.status(429).body(Map.of("error","发送太频繁，请稍后再试"));
    }

    String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
    var rec = new EmailCode();
    rec.setEmail(email);
    rec.setPurpose(PURPOSE_REGISTER);
    rec.setCodeHash(encoder.encode(code));
    rec.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
    codes.save(rec);

    // 发信（MailService 内部已 setFrom 为 spring.mail.username，仅邮箱地址）
    try {
      mail.sendPlain(
          email,
          "【ClassiFlea】注册验证码",
          "您的验证码是：" + code + "（10 分钟内有效）。如果不是发起的请求，请忽略。"
      );
    } catch (RuntimeException ex) {
      // 如果 SMTP 一时失败，提示前端稍后再试
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(Map.of("error", "邮件发送失败，请稍后再试"));
    }

    return ResponseEntity.ok(Map.of("ok", true));
  }

  // 带验证码的注册
  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
    String email = String.valueOf(body.getOrDefault("email","")).trim().toLowerCase();
    String password = String.valueOf(body.getOrDefault("password",""));
    String nickname = String.valueOf(body.getOrDefault("nickname","")).trim();
    String code = String.valueOf(body.getOrDefault("code","")).trim();

    if (email.isEmpty())
      return ResponseEntity.badRequest().body(Map.of("error","邮箱不能为空"));
    if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"))
      return ResponseEntity.badRequest().body(Map.of("error","密码至少8位且包含字母和数字"));
    if (users.findByEmail(email).isPresent())
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","该邮箱已被注册"));
    if (code.isEmpty())
      return ResponseEntity.badRequest().body(Map.of("error","验证码不能为空"));

    // 校验验证码（取最新的一条未使用记录）
    var recOpt = codes.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, PURPOSE_REGISTER);
    if (recOpt.isEmpty())
      return ResponseEntity.badRequest().body(Map.of("error","请先发送验证码"));
    var rec = recOpt.get();
    if (Instant.now().isAfter(rec.getExpiresAt()))
      return ResponseEntity.badRequest().body(Map.of("error","验证码已过期"));
    if (rec.getAttempts() >= 5)
      return ResponseEntity.status(429).body(Map.of("error","尝试次数过多，请重新获取验证码"));
    rec.setAttempts(rec.getAttempts() + 1);
    boolean ok = encoder.matches(code, rec.getCodeHash());
    if (!ok) {
      codes.save(rec);
      return ResponseEntity.badRequest().body(Map.of("error","验证码不正确"));
    }
    rec.setUsed(true);
    codes.save(rec);

    if (nickname.isEmpty()) { // 默认昵称：邮箱前缀
      int at = email.indexOf("@");
      nickname = at > 0 ? email.substring(0, at) : email;
    }

    // 创建用户
    var u = new User();
    u.setEmail(email);
    u.setPasswordHash(encoder.encode(password));
    u.setRole("ROLE_USER");
    u.setNickname(nickname);
    try {
      users.save(u);
    } catch (DataIntegrityViolationException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","该邮箱已被注册"));
    }

    // 同步创建/更新 Profile
    var p = profiles.findByUserEmail(email).orElseGet(() -> {
      var np = new Profile(); np.setUserEmail(email); return np;
    });
    p.setNickname(nickname);
    p.setUpdatedAt(Instant.now());
    profiles.save(p);

    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("ok", true));
  }
}
