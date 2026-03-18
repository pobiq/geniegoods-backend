package com.example.geniegoods.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "TB_UPLOAD_IMG_GROUP")
@EntityListeners(AuditingEntityListener.class)
@ToString
@Builder
public class UploadImgGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long uploadGroupId;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

}
