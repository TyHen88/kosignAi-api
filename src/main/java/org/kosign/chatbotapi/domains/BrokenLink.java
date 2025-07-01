//package org.kosign.chatbotapi.domains;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "broken_links")
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//public class BrokenLink {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String url;
//    private String error;
//
//    @CreationTimestamp
//    private LocalDateTime createdAt;
//
//
//}
