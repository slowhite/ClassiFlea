// src/main/java/com/example/ClassiFlea/MyListingController.java
package com.example.ClassiFlea;

import com.example.ClassiFlea.listing.Listing;
import com.example.ClassiFlea.listing.ListingRepository;
import com.example.ClassiFlea.wallet.WalletSettlementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/my/listings")
public class MyListingController {

  private final ListingRepository repo;
  private final OrderRepository orderRepo;
  private final WalletSettlementService wallet;

  public MyListingController(ListingRepository repo,
                             OrderRepository orderRepo,
                             WalletSettlementService wallet) {
    this.repo = repo;
    this.orderRepo = orderRepo;
    this.wallet = wallet;
  }

  private static String currentUser(Principal principal) {
    if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not login");
    return principal.getName();
  }

  // 我的商品（可带 status）
  @GetMapping
  public List<Listing> mine(Principal principal, @RequestParam(required = false) String status) {
    String me = currentUser(principal);
    return (status == null || status.isBlank())
        ? repo.findBySellerEmailOrderByIdDesc(me)
        : repo.findBySellerEmailAndStatusIgnoreCaseOrderByIdDesc(me, status);
  }

  // 发布（默认 active）
  @PostMapping
  @Transactional
  public ResponseEntity<?> create(Principal principal, @RequestBody Map<String, Object> body) {
    String me = currentUser(principal);

    Listing li = new Listing();
    li.setSellerEmail(me);
    li.setTitle(String.valueOf(body.getOrDefault("title", "未命名")));
    li.setPrice(new BigDecimal(String.valueOf(body.getOrDefault("price", "0"))));
    li.setCampus(String.valueOf(body.getOrDefault("campus", "")));
    li.setCategory(String.valueOf(body.getOrDefault("category", "")));
    li.setConditionLabel(String.valueOf(body.getOrDefault("conditionLabel", "")));
    li.setLocation(String.valueOf(body.getOrDefault("location", "")));
    li.setDescription(String.valueOf(body.getOrDefault("description", "")));
    li.setCoverImage(String.valueOf(body.getOrDefault("coverImage", "dist/img/photo1.png")));
    li.setImagesJson(String.valueOf(body.getOrDefault("imagesJson", "[]")));
    li.setStatus("active");

    return ResponseEntity.ok(repo.save(li));
  }

  @PatchMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(Principal principal,
                                  @PathVariable Long id,
                                  @RequestBody Map<String, Object> body) {
    String me = currentUser(principal);
    var li = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!me.equalsIgnoreCase(li.getSellerEmail())) {
      return ResponseEntity.status(403).body("not owner");
    }

    String oldStatus = String.valueOf(li.getStatus()==null ? "" : li.getStatus()).toLowerCase(Locale.ROOT);
    String newStatus = body.containsKey("status")
        ? String.valueOf(body.get("status")).toLowerCase(Locale.ROOT)
        : oldStatus;

    // 允许改价/标题/描述/图
    if (body.containsKey("price"))       li.setPrice(new BigDecimal(String.valueOf(body.get("price"))));
    if (body.containsKey("title"))       li.setTitle(String.valueOf(body.get("title")));
    if (body.containsKey("description")) li.setDescription(String.valueOf(body.get("description")));
    if (body.containsKey("coverImage"))  li.setCoverImage(String.valueOf(body.get("coverImage")));
    if (body.containsKey("imagesJson"))  li.setImagesJson(String.valueOf(body.get("imagesJson")));

    // 仅支持设置为 closed
    if (body.containsKey("status")) {
      if (!"closed".equals(newStatus) && !"active".equals(newStatus)) {
        // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status not allowed");
      }
      li.setStatus(newStatus);
    }

    // —— 核心：reserved -> closed/active 触发“取消订单 + 退款给买家 + 取消卖家HOLD”
    boolean unreserve = "reserved".equals(oldStatus) &&
                       ("closed".equals(newStatus) || "active".equals(newStatus));
    if (unreserve) {
      orderRepo.findTopByListingIdAndStatusOrderByIdDesc(li.getId(), "PENDING").ifPresent(o -> {
        // 订单改 CANCELLED
        if (!"CANCELLED".equalsIgnoreCase(o.getStatus())) {
          o.setStatus("CANCELLED");
          orderRepo.save(o);
        }
        // 钱包退款
        wallet.refundOnCancel(o.getBuyerEmail(), li.getSellerEmail(), String.valueOf(o.getId()));
      });
    }

    return ResponseEntity.ok(repo.save(li));
  }

  // 删除 listing：如是 reserved 也先退款再删（避免遗留资金占用）
  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(Principal principal, @PathVariable Long id) {
    String me = currentUser(principal);
    var li = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!me.equalsIgnoreCase(li.getSellerEmail())) {
      return ResponseEntity.status(403).body("not owner");
    }

    if ("reserved".equalsIgnoreCase(String.valueOf(li.getStatus()))) {
      orderRepo.findTopByListingIdAndStatusOrderByIdDesc(li.getId(), "PENDING").ifPresent(o -> {
        if (!"CANCELLED".equalsIgnoreCase(o.getStatus())) {
          o.setStatus("CANCELLED");
          orderRepo.save(o);
        }
        wallet.refundOnCancel(o.getBuyerEmail(), li.getSellerEmail(), String.valueOf(o.getId()));
      });
    }

    repo.delete(li);
    return ResponseEntity.noContent().build();
  }
}
