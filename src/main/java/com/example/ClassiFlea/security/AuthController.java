package com.example.ClassiFlea.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
  @GetMapping("/login")
  public String loginPage() {
    return "login"; // 返回 templates/login.html
  }
}
