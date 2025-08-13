// src/main/java/com/example/ClassiFlea/wallet/WalletTxnRepository.java
package com.example.ClassiFlea.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletTxnRepository extends JpaRepository<WalletTxn, Long> {
  List<WalletTxn> findTop20ByUserEmailOrderByCreatedAtDesc(String userEmail);
  boolean existsByUserEmailAndMethodIgnoreCase(String userEmail, String method);

  // 用于查找 HOLD 记录
  Optional<WalletTxn> findFirstByUserEmailAndMethodIgnoreCase(String userEmail, String method);
}
