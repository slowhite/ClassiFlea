// src/main/java/com/example/ClassiFlea/profile/ProfileController.java
package com.example.ClassiFlea.profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ProfileController {
  private final ProfileRepository repo;
  public ProfileController(ProfileRepository repo) { this.repo = repo; }

  // 将可能为 null 的字符串转成空串，避免 NPE 或 Map.of 禁止 null 的问题
  private static String nz(String s) { return s == null ? "" : s; }
  private static String s(Object v)  { return v == null ? "" : String.valueOf(v); }

  @GetMapping("/profile")
  public ResponseEntity<Map<String, Object>> get(Principal me) {
    if (me == null) {
      // 未登录：返回 401 + 统一结构
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "未登录"));
    }

    var p = repo.findByUserEmail(me.getName()).orElse(null);

    // 用可接受 null 的 HashMap 来构造响应
    Map<String, Object> out = new HashMap<>();
    out.put("email", me.getName());
    out.put("nickname",  p != null ? nz(p.getNickname())  : "");
    out.put("avatarUrl", p != null ? nz(p.getAvatarUrl()) : "");
    out.put("phone",     p != null ? nz(p.getPhone())     : "");
    out.put("bio",       p != null ? nz(p.getBio())       : "");
    return ResponseEntity.ok(out);
  }

  @PostMapping("/profile")
  @Transactional
  public ResponseEntity<?> upsert(Principal me, @RequestBody Map<String, Object> body) {
    if (me == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "未登录"));
    }

    var p = repo.findByUserEmail(me.getName()).orElseGet(() -> {
      var np = new Profile();
      np.setUserEmail(me.getName());
      return np;
    });

    p.setNickname(s(body.get("nickname")));
    p.setAvatarUrl(s(body.get("avatarUrl")));
    p.setPhone(s(body.get("phone")));
    p.setBio(s(body.get("bio")));
    p.setUpdatedAt(Instant.now());

    return ResponseEntity.ok(repo.save(p));
  }
}
