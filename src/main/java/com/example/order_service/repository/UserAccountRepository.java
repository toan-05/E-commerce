package com.example.order_service.repository;

import com.example.order_service.entity.UserAccount;
import com.example.order_service.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailAndStatus(String email, RecordStatus status);

    boolean existsByEmailAndStatus(String email, RecordStatus status);
}
