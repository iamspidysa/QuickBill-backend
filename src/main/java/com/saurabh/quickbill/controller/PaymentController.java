package com.saurabh.quickbill.controller;

import com.razorpay.RazorpayException;
import com.saurabh.quickbill.io.OrderResponse;
import com.saurabh.quickbill.io.PaymentRequest;
import com.saurabh.quickbill.io.PaymentVerificationRequest;
import com.saurabh.quickbill.io.RazorpayOrderResponse;
import com.saurabh.quickbill.service.OrderService;
import com.saurabh.quickbill.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final OrderService orderService;

    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.CREATED)
    public RazorpayOrderResponse createRazorpayResponse(@RequestBody PaymentRequest request) throws RazorpayException {

        return razorpayService.createOrder(request.getAmount(), request.getCurrency());
    }

    @PostMapping("/verify")
    public OrderResponse verifyPayment(@RequestBody PaymentVerificationRequest request){
        return orderService.verifyPayment(request);
    }
}
