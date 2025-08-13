// src/main/java/com/example/ClassiFlea/ListingRepository.java
package com.example.ClassiFlea.listing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
  List<Listing> findByStatusIgnoreCase(String status);
  List<Listing> findBySellerEmailOrderByIdDesc(String sellerEmail);
  List<Listing> findBySellerEmailAndStatusIgnoreCaseOrderByIdDesc(String sellerEmail, String status);
  
}
