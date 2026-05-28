package com.saurabh.quickbill.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.saurabh.quickbill.io.OrderResponse;
import com.saurabh.quickbill.io.RazorpayOrderResponse;
import com.saurabh.quickbill.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import static java.lang.Math.round;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    private final RazorpayClient razorpayClient;

    @Override
    public RazorpayOrderResponse createOrder(Double amount, String currency) throws RazorpayException {

        // Temporary — add during testing, remove after
        log.info("RazorpayClient instance: {}", System.identityHashCode(razorpayClient));
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) Math.round(amount * 100));
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", "order_rcptid_"+System.currentTimeMillis());
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
