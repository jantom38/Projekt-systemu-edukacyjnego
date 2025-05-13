package org.example.DataBaseRepositories;

import org.example.database.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleCodeRepository extends JpaRepository<RoleCode, Long> {
    Optional<RoleCode> findByCodeAndIsActiveTrue(String code);
}