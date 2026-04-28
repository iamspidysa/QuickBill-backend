package com.saurabh.quickbill.io;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class ItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 100, message = "Item name must be between 2 and 100 characters long")
    private String name;

    @NotNull(message = "Item price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price must be between 0.01 and 999999.99")
    private BigDecimal price;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
