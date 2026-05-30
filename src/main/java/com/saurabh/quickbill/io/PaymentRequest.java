package com.saurabh.quickbill.io;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    /**
     * The internal order ID (e.g. "ORD-A3F92B1C") created by POST /orders.
     * The server fetches this order and uses its server-computed grandTotal
     * as the Razorpay payment amount. The client never supplies the amount
     * directly — that would allow paying ₹0.01 for any order.
     */
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO 4217 code (e.g. INR)")
    private String currency;
}