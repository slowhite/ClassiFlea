package com.example.ClassiFlea;

import com.example.ClassiFlea.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;

@RestController
public class MeController {
  private final UserRepository users;
  public MeController(UserRepository users){ this.users = users; }

  @GetMapping("/me")
  public Map<String,String> me(Principal p){
    if (p == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    var u = users.findByEmail(p.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return Map.of("email", u.getEmail(), "nickname", u.getNickname() == null ? "" : u.getNickname());
  }
}
