package com.specflux.user.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for User aggregate root. */
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByFirebaseUid(String firebaseUid);

  Optional<User> findByPublicId(String publicId);

  Optional<User> findByEmail(String email);

  boolean existsByFirebaseUid(String firebaseUid);

  boolean existsByEmail(String email);
}
