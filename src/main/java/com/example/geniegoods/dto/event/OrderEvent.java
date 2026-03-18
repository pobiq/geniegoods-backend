package com.example.geniegoods.dto.event;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class OrderEvent {

    private Long orderId;
    private String userId;
    private Long goodsId;
    private int quantity;

}
