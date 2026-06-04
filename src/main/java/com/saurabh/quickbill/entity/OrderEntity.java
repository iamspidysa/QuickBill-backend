package com.saurabh.quickbill.entity;

import com.saurabh.quickbill.io.PaymentDetails;
import com.saurabh.quickbill.io.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tbl_orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", unique = true, nullable = false, updatable = false)
    private String orderId;
    private String customerName;
    private String phoneNumber;
    private BigDecimal subTotal;
    private BigDecimal tax;
    private BigDecimal grandTotal;

    // Who created this order. Populated from the JWT principal at creation time.
    // Used in deleteOrder() to enforce that only the creating user (or an admin)
    // can delete the order. Never sent from the client — always set server-side.
    private String createdByUserId;

    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItemEntity> items = new ArrayList<>();

    @Embedded
    private PaymentDetails paymentDetails;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @PrePersist
    protected void onCreate() {
        // ORD-prefix + first 8 chars of a random UUID in uppercase.
        // e.g. ORD-A3F92B1C
        //
        // Why not System.currentTimeMillis():
        //   Two concurrent requests in the same millisecond produce identical IDs —
        //   a primary business key collision on a billing system.
        //
        // Why 8 hex chars from UUID (not full UUID):
        //   16^8 = ~4.3 billion combinations — statistically impossible to collide
        //   at any realistic order volume, while remaining short enough to print
        //   on a receipt or display in a URL path (/orders/ORD-A3F92B1C).
        this.orderId = "ORD-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
        this.createdAt = LocalDateTime.now();
    }
}