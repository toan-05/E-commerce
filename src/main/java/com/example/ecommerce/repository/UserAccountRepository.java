package com.example.ecommerce.repository;

import com.example.ecommerce.entity.UserAccount;
import com.example.ecommerce.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailAndStatus(String email, RecordStatus status);

    boolean existsByEmail(String email);
}
