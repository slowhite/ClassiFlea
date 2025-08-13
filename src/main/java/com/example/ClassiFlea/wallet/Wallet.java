package com.example.ClassiFlea.wallet;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wallets")
public class Wallet {
  @Id
  @Column(length = 255)
  private String userEmail;

  private long balanceCents;       // 以分存，避免浮点误差
  private Instant updatedAt;

  public Wallet() {}
  public Wallet(String userEmail) {
    this.userEmail = userEmail;
    this.balanceCents = 0L;
    this.updatedAt = Instant.now();
  }

  public String getUserEmail() { return userEmail; }
  public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

  public long getBalanceCents() { return balanceCents; }
  public void setBalanceCents(long balanceCents) { this.balanceCents = balanceCents; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
