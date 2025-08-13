package com.example.ClassiFlea.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailCodeRepository extends JpaRepository<EmailCode, Long> {
  Optional<EmailCode> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, String purpose);
  long deleteAllByEmail(String email);
}
