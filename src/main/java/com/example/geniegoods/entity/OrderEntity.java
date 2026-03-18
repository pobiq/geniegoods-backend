package com.example.geniegoods.entity;

import com.example.geniegoods.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TB_ORDER")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "ORDER_NUMBER", unique = true, length = 50)
    private String orderNumber;

    @CreatedDate
    @Column(name = "ORDERED_AT")
    private LocalDateTime orderedAt;

    @Column(name = "TOTAL_AMOUNT")
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private OrderStatus status;

    @Column(name = "ZIPCODE")
    private String zipcode;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "DETAIL_ADDRESS")
    private String detailAddress;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

}