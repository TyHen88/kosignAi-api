package org.kosign.chatbotapi.repository;

import org.kosign.chatbotapi.domains.Users;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    @Query("SELECT u FROM Users u WHERE u.username = ?1")
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "10"))
    List<Users> findByUsername(String username);
    
    @Cacheable(value = "user-email-cache", key = "#email")
    @Query("SELECT u FROM Users u WHERE u.email = ?1")
    Optional<Users> findByEmail(String email);
}
