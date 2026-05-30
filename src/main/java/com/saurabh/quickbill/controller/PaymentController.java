package com.saurabh.quickbill.controller;

import com.razorpay.RazorpayException;
import com.saurabh.quickbill.io.OrderResponse;
import com.saurabh.quickbill.io.PaymentRequest;
import com.saurabh.quickbill.io.PaymentVerificationRequest;
import com.saurabh.quickbill.io.RazorpayOrderResponse;
import com.saurabh.quickbill.service.OrderService;
import com.saurabh.quickbill.service.RazorpayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final OrderService orderService;

    /**
     * Creates a Razorpay payment order.
     *
     * The amount is NOT taken from the request body. Instead:
     *   1. We fetch the order by orderId from the DB.
     *   2. We use its server-computed grandTotal as the Razorpay amount.
     *
     * This ensures a client cannot manipulate the payment amount by sending
     * a crafted request (e.g. amount: 0.01 for a ₹2,000 order).
     */
    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.CREATED)
    public RazorpayOrderResponse createRazorpayOrder(@Valid @RequestBody PaymentRequest request)
            throws RazorpayException {
        OrderResponse order = orderService.getOrderById(request.getOrderId());
        return razorpayService.createOrder(order.getGrandTotal(), request.getCurrency());
    }

    @PostMapping("/verify")
    public OrderResponse verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {
        return orderService.verifyPayment(request);
    }
}