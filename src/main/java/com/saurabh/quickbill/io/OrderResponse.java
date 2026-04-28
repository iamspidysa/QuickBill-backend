package com.saurabh.quickbill.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {

    private String orderId;
    private String customerName;
    private String phoneNumber;
    private List<OrderResponse.OrderItemResponse> items;
    private BigDecimal subTotal;
    private BigDecimal tax;
    private BigDecimal grandTotal;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
    private PaymentDetails paymentDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private String itemId;
        private String name;
        private BigDecimal price;
        private Integer quantity;
    }

}
