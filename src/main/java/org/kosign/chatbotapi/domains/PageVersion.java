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
//@Entity
//@Table(name = "page_versions")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class PageVersion {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne
//    @JoinColumn(name = "page_id")
//    private PageContent page;
//
//    private String url;
//    private String title;
//
//    @Column(columnDefinition = "TEXT")
//    private String content;
//
//    private String contentHash;
//    private Integer status;
//    private Integer depth;
//
//    @CreationTimestamp
//    private LocalDateTime createdAt;
//}
