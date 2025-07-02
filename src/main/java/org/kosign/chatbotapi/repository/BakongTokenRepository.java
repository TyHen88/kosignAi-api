// package org.kosign.chatbotapi.repository;

// import org.kosign.chatbotapi.model.BakongToken;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Modifying;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
// import org.springframework.stereotype.Repository;

// import java.time.LocalDateTime;
// import java.util.Optional;

// /**
//  * Simplified repository interface for BakongToken entity
//  */
// @Repository
// public interface BakongTokenRepository extends JpaRepository<BakongToken, Long> {
    
//     /**
//      * Find existing token for a service and email
//      */
//     @Query("SELECT bt FROM BakongToken bt WHERE bt.serviceName = :serviceName " +
//            "AND bt.email = :email ORDER BY bt.createdAt DESC")
//     Optional<BakongToken> findByServiceAndEmail(
//         @Param("serviceName") String serviceName, 
//         @Param("email") String email
//     );
    
//     /**
//      * Find valid (not expired) token for a service and email
//      */
//     @Query("SELECT bt FROM BakongToken bt WHERE bt.serviceName = :serviceName " +
//            "AND bt.email = :email AND bt.expiresAt > :currentTime " +
//            "ORDER BY bt.createdAt DESC")
//     Optional<BakongToken> findValidToken(
//         @Param("serviceName") String serviceName, 
//         @Param("email") String email,
//         @Param("currentTime") LocalDateTime currentTime
//     );
    
//     /**
//      * Find valid token with buffer time for a service and email
//      */
//     @Query("SELECT bt FROM BakongToken bt WHERE bt.serviceName = :serviceName " +
//            "AND bt.email = :email AND bt.expiresAt > :bufferTime " +
//            "ORDER BY bt.createdAt DESC")
//     Optional<BakongToken> findValidTokenWithBuffer(
//         @Param("serviceName") String serviceName, 
//         @Param("email") String email,
//         @Param("bufferTime") LocalDateTime bufferTime
//     );
    
//     /**
//      * Delete existing token for a service and email (to replace with new one)
//      */
//     @Modifying
//     @Query("DELETE FROM BakongToken bt WHERE bt.serviceName = :serviceName AND bt.email = :email")
//     int deleteByServiceAndEmail(
//         @Param("serviceName") String serviceName, 
//         @Param("email") String email
//     );
    
//     /**
//      * Clean up expired tokens
//      */
//     @Modifying
//     @Query("DELETE FROM BakongToken bt WHERE bt.expiresAt < :currentTime")
//     int deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);
    
//     /**
//      * Check if valid token exists for service and email
//      */
//     @Query("SELECT COUNT(bt) > 0 FROM BakongToken bt WHERE bt.serviceName = :serviceName " +
//            "AND bt.email = :email AND bt.expiresAt > :currentTime")
//     boolean hasValidToken(
//         @Param("serviceName") String serviceName,
//         @Param("email") String email,
//         @Param("currentTime") LocalDateTime currentTime
//     );
// } 