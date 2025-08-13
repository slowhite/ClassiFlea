// src/main/java/com/example/ClassiFlea/admin/AdminUserController.java
package com.example.ClassiFlea.admin;

import com.example.ClassiFlea.user.UserRepository;
import com.example.ClassiFlea.profile.ProfileRepository;
import com.example.ClassiFlea.account.EmailCodeRepository;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final UserRepository users;
  private final ProfileRepository profiles;
  private final EmailCodeRepository codes;

  public AdminUserController(UserRepository u, ProfileRepository p, EmailCodeRepository c){
    this.users = u; this.profiles = p; this.codes = c;
  }

  @DeleteMapping("/{email}")
  public ResponseEntity<Void> deleteByEmail(@PathVariable String email){
    String e = email.trim().toLowerCase();
    var u = users.findByEmail(e);
    if (u.isEmpty()) return ResponseEntity.notFound().build();

    profiles.deleteByUserEmail(e);
    codes.deleteAllByEmail(e);


    users.delete(u.get());
    return ResponseEntity.noContent().build();
  }
}
