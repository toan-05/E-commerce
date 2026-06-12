package com.example.order_service.repository;

import com.example.order_service.entity.Permission;
import com.example.order_service.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCodeAndStatus(String code, RecordStatus status);

    boolean existsByCodeAndStatus(String code, RecordStatus status);
}
