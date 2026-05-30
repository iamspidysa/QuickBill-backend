package com.saurabh.quickbill.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.saurabh.quickbill.io.RazorpayOrderResponse;
import com.saurabh.quickbill.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    private final RazorpayClient razorpayClient;

    @Override
    public RazorpayOrderResponse createOrder(BigDecimal amount, String currency) throws RazorpayException {
        // Razorpay expects amounts in the smallest currency unit (paise for INR).
        // Using BigDecimal multiply + HALF_UP avoids the floating-point errors
        // that (int)(double * 100) produces for values like 1.005 or 99.999.
        int amountInPaise = amount
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", "order_rcptid_" + System.currentTimeMillis());
        orderRequest.put("payment_capture", 1);

        Order order = razorpayClient.orders.create(orderRequest);
        return convertToResponse(order);
    }

    private RazorpayOrderResponse convertToResponse(Order order) {
        return RazorpayOrderResponse.builder()
                .id(order.get("id"))
                .entity(order.get("entity"))
                .amount(order.get("amount"))
                .currency(order.get("currency"))
                .status(order.get("status"))
                .created_at(order.get("created_at"))
                .receipt(order.get("receipt"))
                .build();
    }
}