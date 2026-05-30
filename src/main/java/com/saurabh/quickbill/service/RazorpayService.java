package com.saurabh.quickbill.service;

import com.razorpay.RazorpayException;
import com.saurabh.quickbill.io.RazorpayOrderResponse;

import java.math.BigDecimal;

public interface RazorpayService {

    /**
     * @param amount   Server-computed order total (BigDecimal, not Double).
     *                 Using BigDecimal avoids floating-point rounding errors
     *                 when converting rupees to paise (× 100).
     * @param currency ISO 4217 code, e.g. "INR"
     */
    RazorpayOrderResponse createOrder(BigDecimal amount, String currency) throws RazorpayException;
}