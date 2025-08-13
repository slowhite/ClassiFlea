package com.example.ClassiFlea.account;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="email_codes", indexes=@Index(name="idx_email_purpose", columnList="email,purpose"))
public class EmailCode {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

  @Column(nullable=false, length=120) private String email;
  @Column(nullable=false, length=20)  private String purpose;   // e.g. REGISTER
  @Column(nullable=false, length=200) private String codeHash;  // 不明文存储
  @Column(nullable=false)             private Instant expiresAt; // 10 分钟
  @Column(nullable=false)             private Instant createdAt = Instant.now();
  @Column(nullable=false)             private int attempts = 0; // 校验次数
  @Column(nullable=false)             private boolean used = false;

  // getters/setters
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getEmail(){return email;} public void setEmail(String v){this.email=v;}
  public String getPurpose(){return purpose;} public void setPurpose(String v){this.purpose=v;}
  public String getCodeHash(){return codeHash;} public void setCodeHash(String v){this.codeHash=v;}
  public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){this.expiresAt=v;}
  public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){this.createdAt=v;}
  public int getAttempts(){return attempts;} public void setAttempts(int v){this.attempts=v;}
  public boolean isUsed(){return used;} public void setUsed(boolean v){this.used=v;}
}
