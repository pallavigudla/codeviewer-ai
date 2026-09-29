package com.codereviewagent.repository;

import com.codereviewagent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findByEmailAndIsEmailVerifiedTrue(String email);
    Optional<User> findByUsernameAndIsEmailVerifiedTrue(String username);
    boolean existsByEmailAndIsEmailVerifiedTrue(String email);
    boolean existsByUsernameAndIsEmailVerifiedTrue(String username);
}
