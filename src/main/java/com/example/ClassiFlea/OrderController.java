package com.example.ClassiFlea;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.ClassiFlea.listing.Listing;
import com.example.ClassiFlea.listing.ListingRepository;
import com.example.ClassiFlea.wallet.WalletSettlementService;  

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderRepository orderRepo;
  private final ListingRepository listingRepo;
  private final WalletSettlementService walletSettlement;      

  public OrderController(OrderRepository orderRepo,
                         ListingRepository listingRepo,
                         WalletSettlementService walletSettlement) {  
    this.orderRepo = orderRepo;
    this.listingRepo = listingRepo;
    this.walletSettlement = walletSettlement;                 
  }

  /* 只返回我买到的订单（前端 /orders?mine=1 也会打到这里） */
  @GetMapping
  public List<Order> myOrders(Principal me) {
    ensureLogin(me);
    return orderRepo.findByBuyerEmailOrderByIdDesc(me.getName());
  }

  /* 创建订单：绑定 buyer、商品置为 reserved，并立刻扣买家余额 + 给卖家生成挂起收入 */
  @PostMapping
  @Transactional
  public ResponseEntity<?> create(Principal me, @RequestBody Map<String, Object> body) {
    ensureLogin(me);

    // 1) 取 listingId 并校验
    Long listingId = toLong(body.get("listingId"))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "listingId required"));

    Listing li = listingRepo.findById(listingId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "listing not found"));

    // 2) 只能给 active 的商品下单
    if (!"active".equalsIgnoreCase(li.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "listing not active");
    }

    // 3) 禁止给自己商品下单
    if (li.getSellerEmail() != null && li.getSellerEmail().equalsIgnoreCase(me.getName())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "cannot buy your own listing");
    }

    // 4) 创建订单并绑定当前买家
    Order o = new Order();
    o.setListingId(listingId);
    o.setStatus("PENDING");
    o.setBuyerEmail(me.getName());
    o.setBuyerName(String.valueOf(body.getOrDefault("buyerName", "")));
    o.setContact(String.valueOf(body.getOrDefault("contact", "")));
    o.setNote(String.valueOf(body.getOrDefault("note", "")));

    // 5) 商品置为 reserved
    li.setStatus("reserved");
    listingRepo.save(li);

    // 6) 先保存订单拿到 id
    o = orderRepo.save(o);

    // 7) 钱包结算：立即扣买家 + 卖家生成挂起收入（不入账）
    BigDecimal priceYuan = Optional.ofNullable(li.getPrice())
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "price missing"));
if (priceYuan.signum() <= 0) {
  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be > 0");
}

// 元 → 分，四舍五入到“分”
long amountCents = priceYuan
    .movePointRight(2)                 // 小数点右移 2 位，相当于 *100
    .setScale(0, RoundingMode.HALF_UP) // 保留到整数分
    .longValueExact();                 // 金额超出 long 时会抛异常
 
    walletSettlement.chargeForOrder(
        me.getName(),                 // buyer
        li.getSellerEmail(),          // seller
        String.valueOf(o.getId()),    // orderId
        amountCents
    );

    return ResponseEntity.ok(o);
  }

  @PostMapping("/{id}/cancel")
  @Transactional
  public ResponseEntity<?> cancel(@PathVariable Long id, Principal me) {
    ensureLogin(me);
    Order o = orderRepo.findByIdAndBuyerEmail(id, me.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "not your order"));

    o.setStatus("CANCELLED");
    Listing li = listingRepo.findById(o.getListingId()).orElse(null);
    if (li != null) {
      li.setStatus("active");
      listingRepo.save(li);
      //  钱包：退款 & 取消卖家挂起收入
      walletSettlement.refundOnCancel(
          o.getBuyerEmail(),
          li.getSellerEmail(),
          String.valueOf(o.getId())
      );
    }

    return ResponseEntity.ok(orderRepo.save(o));
  }

  /* 确认成交：商品置为 sold，把卖家挂起收入入账 */
  @PostMapping("/{id}/confirm")
  @Transactional
  public ResponseEntity<?> confirm(@PathVariable Long id, Principal me) {
    ensureLogin(me);
    Order o = orderRepo.findByIdAndBuyerEmail(id, me.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "not your order"));

    o.setStatus("CONFIRMED");
    Listing li = listingRepo.findById(o.getListingId()).orElse(null);
    if (li != null) {
      li.setStatus("sold");
      listingRepo.save(li);
      //  钱包：释放卖家挂起收入，正式入账
      walletSettlement.releaseOnComplete(
          li.getSellerEmail(),
          String.valueOf(o.getId())
      );
    }

    return ResponseEntity.ok(orderRepo.save(o));
  }

  /* 删除记录：只能删自己的订单（不动钱包历史，将作为审计保留） */
  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> remove(@PathVariable Long id, Principal me) {
    ensureLogin(me);
    Order o = orderRepo.findByIdAndBuyerEmail(id, me.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "not your order"));

    orderRepo.delete(o);
    return ResponseEntity.noContent().build();
  }

  // ------- 小工具 --------
  private static void ensureLogin(Principal me) {
    if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  }

  private static Optional<Long> toLong(Object v) {
    try {
      if (v == null) return Optional.empty();
      if (v instanceof Number) return Optional.of(((Number) v).longValue());
      String s = String.valueOf(v).trim();
      if (s.isEmpty()) return Optional.empty();
      return Optional.of(Long.parseLong(s));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
