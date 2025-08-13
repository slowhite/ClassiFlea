package com.example.ClassiFlea;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByContactOrderByIdDesc(String contact);

    // 按状态查询
    List<Order> findByBuyerEmailOrderByIdDesc(String buyerEmail);

    
    Optional<Order> findByIdAndBuyerEmail(Long id, String buyerEmail);
    Optional<Order> findTopByListingIdAndStatusOrderByIdDesc(Long listingId, String status);
}
