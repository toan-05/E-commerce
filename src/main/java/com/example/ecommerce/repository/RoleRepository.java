package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCodeAndStatus(String code, RecordStatus status);
}
