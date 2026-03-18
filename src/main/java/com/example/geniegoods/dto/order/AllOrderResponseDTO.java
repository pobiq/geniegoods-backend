package com.example.geniegoods.dto.order;

import com.example.geniegoods.entity.OrderEntity;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AllOrderResponseDTO {
    private Long orderId;
    private String orderNumber;
    private String orderedAt;
    private String goodsUrl;
    private String orderTitle;
    private String status;           // 주문 상태
    private Integer totalAmount;      // 총 금액

    public static AllOrderResponseDTO of(OrderEntity order) {


        String orderedAt = order.getOrderedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return AllOrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .orderedAt(orderedAt)
                .orderTitle("테스트")
                .goodsUrl(null)
                .status(order.getStatus() != null ? order.getStatus().getDescription() : null)
                .totalAmount(order.getTotalAmount())
                .build();
    }
}
