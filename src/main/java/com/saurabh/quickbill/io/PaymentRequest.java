package com.saurabh.quickbill.io;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least ₹1")
    private Double amount;

    @NotBlank(message = "Currency is required")
    private String currency;
}
