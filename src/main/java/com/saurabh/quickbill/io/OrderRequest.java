package com.saurabh.quickbill.io;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequest {

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String phoneNumber;

    @NotEmpty(message = "Cart must have at least one item")
    @Valid                          // Cascade validation into each OrderItemRequest
    private List<OrderItemRequest> cartItems;

    @NotNull(message = "Sub total is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Sub total must be greater than zero")
    private BigDecimal subTotal;    // Changed from Double — use BigDecimal for money

    @NotNull(message = "Tax is required")
    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    private BigDecimal tax;

    @NotNull(message = "Grand total is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Grand total must be greater than zero")
    private BigDecimal grandTotal;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(CASH|UPI)$", message = "Payment method must be CASH or UPI")
    private String paymentMethod;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OrderItemRequest{

        @NotBlank(message = "Item ID is required")
        private String itemId;

        @NotBlank(message = "Item name is required")
        private String name;

        @NotNull(message = "Item price is required")
        @DecimalMin(value = "0.01", message = "Item price must be greater than zero")
        private BigDecimal price;   // Changed from Double

        @NotNull(message = "Quantity is required")
        @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
