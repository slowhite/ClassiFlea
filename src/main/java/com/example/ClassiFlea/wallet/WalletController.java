// src/main/java/com/example/ClassiFlea/wallet/WalletController.java
package com.example.ClassiFlea.wallet;

import com.example.ClassiFlea.account.MailService;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/wallet")
public class WalletController {
  private final WalletRepository wallets;
  private final WalletTxnRepository txns;
  private final MailService mail;

  public WalletController(WalletRepository wallets, WalletTxnRepository txns, MailService mail) {
    this.wallets = wallets; this.txns = txns; this.mail = mail;
  }

  private String me(Principal p) {
    if (p == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return p.getName().toLowerCase(Locale.ROOT);
  }

  private Wallet ensure(String email){
    return wallets.findById(email).orElseGet(() -> wallets.save(new Wallet(email)));
  }

  @GetMapping
  public Map<String,Object> myWallet(Principal p){
    var email = me(p);
    var w = ensure(email);
    return Map.of(
      "balance", w.getBalanceCents() / 100.0,
      "currency", "CNY",
      "updatedAt", w.getUpdatedAt()
    );
  }

  /* 我的流水：默认不返回 SALE_HOLD（挂起收入），需要时 ?includePending=1 */
  @GetMapping("/txns")
  public List<Map<String,Object>> myTxns(
      Principal p,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(value = "includePending", required = false) String includePending
  ){
    var email = me(p);
    var list = txns.findTop20ByUserEmailOrderByCreatedAtDesc(email);
    if (limit > 0 && limit < list.size()) list = list.subList(0, limit);

    boolean showPending = "1".equals(includePending) || "true".equalsIgnoreCase(includePending);

    return list.stream()
      //  默认隐藏挂起流水（SALE_HOLD）
      .filter(t -> showPending || !"SALE_HOLD".equalsIgnoreCase(t.getType()))
      .map(t -> Map.<String,Object>of(
        "id", t.getId(),
        "type", t.getType(),
        "amount", t.getAmountCents() / 100.0,
        "method", t.getMethod(),
        "desc", Optional.ofNullable(t.getDescription()).orElse(""),
        "status", t.getStatus(),
        "createdAt", t.getCreatedAt()
      ))
      .collect(Collectors.toList());
  }

  // 兼容 /tx
  @GetMapping("/tx")
  public List<Map<String,Object>> myTxnsAlias(
      Principal p,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(value = "includePending", required = false) String includePending
  ){
    return myTxns(p, limit, includePending);
  }

  // 充值
  @PostMapping("/topup")
  @Transactional
  public Map<String,Object> topup(@RequestBody Map<String,Object> body, Principal p){
    var email = me(p);
    var w = ensure(email);

    long cents = parseCents(body);
    if (cents <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount invalid");
    if (cents > 100_000_00L) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount too big");

    var method = String.valueOf(body.getOrDefault("method","CAMPUS_CARD")).trim();
    var desc   = String.valueOf(body.getOrDefault("description","Top-up via " + method));

    w.setBalanceCents(w.getBalanceCents() + cents);
    w.setUpdatedAt(Instant.now());
    wallets.save(w);

    var t = new WalletTxn();
    t.setUserEmail(email);
    t.setType("TOPUP");
    t.setAmountCents(cents);
    t.setMethod(method);
    t.setDescription(desc);
    t.setStatus("SUCCESS");
    txns.save(t);

    try {
      mail.sendPlain(email, "ClassiFlea | Top-up success",
          "Your wallet has been topped up by ¥" + (cents/100.0) + ".\nThis is a demo receipt.");
    } catch (Exception ignore) {}

    return Map.of("ok", true, "balance", w.getBalanceCents()/100.0);
  }

  // 兑换码：CLASSIFLEA100 / WELCOME5
  @PostMapping("/redeem")
  @Transactional
  public Map<String,Object> redeem(@RequestBody Map<String,String> body, Principal p){
    var email = me(p);
    var w = ensure(email);

    var code = String.valueOf(body.getOrDefault("code","")).trim().toUpperCase(Locale.ROOT);
    if (code.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code required");

    long bonus;
    switch (code){
      case "CLASSIFLEA100" -> bonus = 100_00L; // ¥100
      case "WELCOME5"     -> bonus = 5_00L;   // ¥5
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid code");
    }

    var method = "PROMO:" + code;
    if (txns.existsByUserEmailAndMethodIgnoreCase(email, method))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "code already redeemed");

    w.setBalanceCents(w.getBalanceCents() + bonus);
    w.setUpdatedAt(Instant.now());
    wallets.save(w);

    var t = new WalletTxn();
    t.setUserEmail(email);
    t.setType("TOPUP");
    t.setAmountCents(bonus);
    t.setMethod(method);
    t.setDescription("Promo " + code);
    t.setStatus("SUCCESS");
    txns.save(t);

    return Map.of("ok", true, "balance", w.getBalanceCents()/100.0);
  }

  // --------- 工具 ----------
  private long parseCents(Map<String,Object> body){
    Object ac = body.get("amountCents");
    if (ac != null) {
      try { return Long.parseLong(String.valueOf(ac)); } catch (Exception ignore) {}
    }
    Object ay = (body.containsKey("amountYuan") ? body.get("amountYuan") : body.get("amount"));
    if (ay != null) {
      try {
        double yuan = Double.parseDouble(String.valueOf(ay));
        return Math.round(yuan * 100.0);
      } catch (Exception ignore) {}
    }
    return 0;
  }
}
