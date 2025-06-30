package org.kosign.chatbotapi.repository;

import org.kosign.chatbotapi.domains.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<Users, Long> {
    @Query("SELECT u FROM Users u WHERE u.username = ?1")
    List<Users> findByUsername(String username);
}
