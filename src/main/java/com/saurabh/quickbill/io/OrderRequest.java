package com.saurabh.quickbill.io;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Valid
    private List<OrderItemRequest> cartItems;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(CASH|UPI)$", message = "Payment method must be CASH or UPI")
    private String paymentMethod;

    /*
     * subTotal, tax, and grandTotal have been intentionally removed.
     * These are computed server-side from DB item prices and must never
     * be trusted from the client. See OrderServiceImpl.createOrder().
     */

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OrderItemRequest {

        @NotBlank(message = "Item ID is required")
        private String itemId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Quantity cannot exceed 100 per line item")
        private Integer quantity;

        /*
         * name and price have been intentionally removed.
         * The server fetches these from tbl_items by itemId.
         * Accepting them from the client would allow price manipulation.
         */
    }
}