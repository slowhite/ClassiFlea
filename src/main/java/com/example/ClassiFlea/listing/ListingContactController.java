// src/main/java/com/example/ClassiFlea/listing/ListingContactController.java
package com.example.ClassiFlea.listing;

import com.example.ClassiFlea.account.MailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/listings")
public class ListingContactController {
  private final ListingRepository listings;
  private final MailService mail;

  public ListingContactController(ListingRepository listings, MailService mail) {
    this.listings = listings;
    this.mail = mail;
  }

  /**
   * 发送询问：POST /listings/{id}/ask  { "message": "想问…" }
   * 要求已登录；邮件会发给卖家，Reply-To 设为买家邮箱，卖家直接“回复”即可回到买家。
   */
  @PostMapping("/{id}/ask")
  public ResponseEntity<?> askSeller(@PathVariable Long id,
                                     @RequestBody(required = false) AskRequest body,
                                     Principal me) {
    if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

    var listing = listings.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "listing not found"));

    // 当前登录者（买家）
    String buyerEmail = String.valueOf(me.getName()).trim();

    // 买家留言（可空；限制长度）
    String msg = (body != null && body.message() != null) ? body.message().trim() : "";
    if (msg.length() > 1000) msg = msg.substring(0, 1000);

    String sellerEmail = listing.getSellerEmail();
    if (sellerEmail == null || sellerEmail.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seller email missing");
    }

    String title = safe(listing.getTitle());
    String subject = "【ClassiFlea】买家留言 / New inquiry – \"" + title + "\" (#" + listing.getId() + ")";

    String safeMsg = (msg.isBlank() ? "（对方未填写留言 / No message）" : msg);

    // 中英双语正文（卖家收到）
    String bodyText = """

卖家您好，
买家 %s 对您的商品「%s」（ID: %s）留言：

%s

直接“回复此邮件”即可联系买家（将发送到：%s）。
如该商品已售出，请在「卖出商品」里将状态标记为已售出。

----------------------------

【English】

Hi seller,

Buyer %s asked about your listing "%s" (ID: %s):

%s

Just reply to this email to contact the buyer (your reply goes to: %s).
If the item has been sold, please mark it as Sold in "My Listings".

— ClassiFlea Bot
""".formatted(
        buyerEmail, title, listing.getId(), safeMsg, buyerEmail,
        buyerEmail, title, listing.getId(), safeMsg, buyerEmail
    );

    try {
      mail.sendPlain(sellerEmail, subject, bodyText, buyerEmail);
      return ResponseEntity.ok(Map.of("ok", true));
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "send mail failed");
    }
  }

  private static String safe(Object v) {
    return v == null ? "" : String.valueOf(v);
  }

  public static record AskRequest(String message) {}
}
