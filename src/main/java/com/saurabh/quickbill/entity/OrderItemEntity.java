package com.saurabh.quickbill.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "tbl_orders_item")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String itemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;

}
