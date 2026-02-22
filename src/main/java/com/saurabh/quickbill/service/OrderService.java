package com.saurabh.quickbill.service;

import com.saurabh.quickbill.io.OrderRequest;
import com.saurabh.quickbill.io.OrderResponse;
import com.saurabh.quickbill.io.PaymentVerificationRequest;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    void deleteOrder(String orderId);

    List<OrderResponse> getLatestOrders();

    OrderResponse verifyPayment(PaymentVerificationRequest request);
}
