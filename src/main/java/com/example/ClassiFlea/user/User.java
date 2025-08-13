package com.example.ClassiFlea.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String email;

  @Column(nullable = false, length = 200)
  private String passwordHash;

  @Column(nullable = false, length = 20)
  private String role = "ROLE_USER";

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();
  @Column(length = 60)
  private String nickname;

  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }

  // getters/setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
