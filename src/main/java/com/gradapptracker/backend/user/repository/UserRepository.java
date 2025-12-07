package com.gradapptracker.backend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gradapptracker.backend.user.entity.User;

import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * Spring Data JPA repository for User entity.
 * <p>
 * Provides CRUD operations and custom query methods for user management
 * including email lookup, name search, and existence checks.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    @NonNull
    Optional<User> findById(@NonNull Integer id);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByEmailContainingIgnoreCase(String emailPart);

    boolean existsByEmail(String email);

}
