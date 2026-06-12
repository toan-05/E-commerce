package com.example.order_service.repository;

import com.example.order_service.entity.Role;
import com.example.order_service.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCodeAndStatus(String code, RecordStatus status);
}
