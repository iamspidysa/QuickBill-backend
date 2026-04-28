package com.saurabh.quickbill.io;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters long")
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters long")
    private String description;

    @Size(max = 20, message = "Background color value too long")
    private String bgColor;
}
