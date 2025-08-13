package com.example.ClassiFlea.wallet;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {

  // for update 锁住钱包行，避免并发读写
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select w from Wallet w where w.userEmail = :email")
  Optional<Wallet> findForUpdate(@Param("email") String email);
}
