// src/main/java/com/example/ClassiFlea/profile/ProfileRepository.java
package com.example.ClassiFlea.profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
  Optional<Profile> findByUserEmail(String userEmail);
  long deleteByUserEmail(String email);
}
