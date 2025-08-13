// src/main/java/com/example/ClassiFlea/wallet/WalletSettlementService.java
package com.example.ClassiFlea.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;

@Service
public class WalletSettlementService {

  private final WalletRepository wallets;
  private final WalletTxnRepository txns;

  public WalletSettlementService(WalletRepository wallets, WalletTxnRepository txns) {
    this.wallets = wallets;
    this.txns = txns;
  }

  private Wallet ensure(String email) {
    String key = String.valueOf(email).toLowerCase(Locale.ROOT);
    return wallets.findById(key).orElseGet(() -> wallets.save(new Wallet(key)));
  }

  /* 下单：立即扣买家余额；给卖家生成“挂起收入”（不入账，流水状态为 PENDING） */
  @Transactional
  public void chargeForOrder(String buyerEmail, String sellerEmail, String orderId, long amountCents) {
    if (amountCents <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
    String buyer = buyerEmail.toLowerCase(Locale.ROOT);
    String seller = sellerEmail.toLowerCase(Locale.ROOT);

    String chargeMethod = "ORDER#" + orderId;
    if (txns.existsByUserEmailAndMethodIgnoreCase(buyer, chargeMethod)) return;

    // 1) 扣买家
    Wallet bw = ensure(buyer);
    if (bw.getBalanceCents() < amountCents) {
      throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "insufficient balance");
    }
    bw.setBalanceCents(bw.getBalanceCents() - amountCents);
    bw.setUpdatedAt(Instant.now());
    wallets.save(bw);

    WalletTxn bt = new WalletTxn();
    bt.setUserEmail(buyer);
    bt.setType("PURCHASE");
    bt.setAmountCents(-amountCents);
    bt.setMethod(chargeMethod);
    bt.setDescription("Order #" + orderId);
    bt.setStatus("SUCCESS");
    txns.save(bt);

    // 2) 卖家挂起（不入账）
    String holdMethod = "HOLD#" + orderId;
    if (!txns.existsByUserEmailAndMethodIgnoreCase(seller, holdMethod)) {
      WalletTxn st = new WalletTxn();
      st.setUserEmail(seller);
      st.setType("SALE_HOLD");
      st.setAmountCents(amountCents);
      st.setMethod(holdMethod);
      st.setDescription("Hold for order #" + orderId);
      st.setStatus("PENDING");
      txns.save(st);
    }
  }

  /** 取消：买家退款；卖家的 HOLD 置为 CANCELLED */
  @Transactional
  public void refundOnCancel(String buyerEmail, String sellerEmail, String orderId) {
    String buyer  = buyerEmail.toLowerCase(Locale.ROOT);
    String seller = sellerEmail.toLowerCase(Locale.ROOT);

    String holdMethod = "HOLD#" + orderId;
    var holdOpt = txns.findFirstByUserEmailAndMethodIgnoreCase(seller, holdMethod);
    if (holdOpt.isEmpty()) return;
    var hold = holdOpt.get();
    long cents = hold.getAmountCents();

    String refundMethod = "REFUND#" + orderId;
    if (!txns.existsByUserEmailAndMethodIgnoreCase(buyer, refundMethod)) {
      Wallet bw = ensure(buyer);
      bw.setBalanceCents(bw.getBalanceCents() + cents);
      bw.setUpdatedAt(Instant.now());
      wallets.save(bw);

      WalletTxn rt = new WalletTxn();
      rt.setUserEmail(buyer);
      rt.setType("REFUND");
      rt.setAmountCents(cents);
      rt.setMethod(refundMethod);
      rt.setDescription("Refund for order #" + orderId);
      rt.setStatus("SUCCESS");
      txns.save(rt);
    }

    if (!"CANCELLED".equalsIgnoreCase(hold.getStatus())) {
      hold.setStatus("CANCELLED");
      txns.save(hold);
    }
  }

  /** 成交：释放 HOLD → 卖家余额入账；HOLD 标记为 RELEASED */
  @Transactional
  public void releaseOnComplete(String sellerEmail, String orderId) {
    String seller = sellerEmail.toLowerCase(Locale.ROOT);

    String holdMethod = "HOLD#" + orderId;
    var holdOpt = txns.findFirstByUserEmailAndMethodIgnoreCase(seller, holdMethod);
    if (holdOpt.isEmpty()) return;
    var hold = holdOpt.get();
    if ("RELEASED".equalsIgnoreCase(hold.getStatus())) return;

    long cents = hold.getAmountCents();
    String saleMethod = "SALE#" + orderId;

    // 入账
    if (!txns.existsByUserEmailAndMethodIgnoreCase(seller, saleMethod)) {
      Wallet sw = ensure(seller);
      sw.setBalanceCents(sw.getBalanceCents() + cents);
      sw.setUpdatedAt(Instant.now());
      wallets.save(sw);

      WalletTxn st = new WalletTxn();
      st.setUserEmail(seller);
      st.setType("SALE");
      st.setAmountCents(cents);
      st.setMethod(saleMethod);
      st.setDescription("Sale #" + orderId);
      st.setStatus("SUCCESS");
      txns.save(st);
    }

    // 标记 HOLD 已释放
    hold.setStatus("RELEASED");
    txns.save(hold);
  }
}
