package com.example.ClassiFlea.wallet;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wallet_txn")
public class WalletTxn {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 255, nullable = false)
  private String userEmail;

  @Column(length = 32, nullable = false)
  private String type;       // TOPUP / PURCHASE / REFUND / ADJUST

  private long amountCents;  // 正数信用入账，负数扣款（本 demo 只用正数 TOPUP）

  @Column(length = 128)
  private String method;     // e.g. CAMPUS_CARD / PROMO:CLASSIFLEA100

  @Column(length = 255)
  private String description;

  @Column(length = 32, nullable = false)
  private String status;     // SUCCESS

  private Instant createdAt;

  public WalletTxn() {
    this.createdAt = Instant.now();
    this.status = "SUCCESS";
  }

  // getters/setters
  public Long getId() { return id; }
  public String getUserEmail() { return userEmail; }
  public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public long getAmountCents() { return amountCents; }
  public void setAmountCents(long amountCents) { this.amountCents = amountCents; }
  public String getMethod() { return method; }
  public void setMethod(String method) { this.method = method; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  
}
