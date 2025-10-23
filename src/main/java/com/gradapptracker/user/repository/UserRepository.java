package com.gradapptracker.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gradapptracker.user.entity.User;

import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    @NonNull
    Optional<User> findById(@NonNull Integer id);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByEmailContainingIgnoreCase(String emailPart);

    boolean existsByEmail(String email);

}
