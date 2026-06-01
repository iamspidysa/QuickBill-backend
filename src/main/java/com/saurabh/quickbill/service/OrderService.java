package com.saurabh.quickbill.service;

import com.saurabh.quickbill.io.OrderRequest;
import com.saurabh.quickbill.io.OrderResponse;
import com.saurabh.quickbill.io.PaymentVerificationRequest;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    void deleteOrder(String orderId);

    //    List<OrderResponse> getLatestOrders();

    // Changed: now returns Page<OrderResponse> instead of List<OrderResponse>.
    // Callers pass page number and size; the Page wrapper carries total count,
    // total pages, and whether there is a next page — all the metadata the
    // frontend needs to drive pagination or infinite scroll.
    Page<OrderResponse> getLatestOrders(int page, int size);

    OrderResponse verifyPayment(PaymentVerificationRequest request);

    Double sumSalesByDate(LocalDate date);

    Long countByOrderDate(LocalDate date);

    List<OrderResponse> findRecentOrders();

    /**
     * Fetch a single order by its business key.
     * Used by PaymentController to retrieve the server-computed grandTotal
     * before creating a Razorpay order — prevents clients from supplying
     * an arbitrary amount to the payment gateway.
     */
    OrderResponse getOrderById(String orderId);
}