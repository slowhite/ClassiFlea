package com.example.ClassiFlea.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InitUsers {
  @Bean
  CommandLineRunner init(UserRepository repo, PasswordEncoder enc) {
    return args -> {
      if (repo.count() == 0) {
        User a = new User();
        a.setEmail("a@example.com");
        a.setPasswordHash(enc.encode("pass1234"));
        a.setRole("ROLE_USER");
        repo.save(a);

        User b = new User();
        b.setEmail("b@example.com");
        b.setPasswordHash(enc.encode("pass1234"));
        b.setRole("ROLE_USER");
        repo.save(b);
      }
    };
  }
}
