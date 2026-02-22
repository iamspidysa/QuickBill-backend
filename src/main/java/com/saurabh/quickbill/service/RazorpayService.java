package com.saurabh.quickbill.service;

import com.razorpay.RazorpayException;
import com.saurabh.quickbill.io.RazorpayOrderResponse;

public interface RazorpayService {

    RazorpayOrderResponse createOrder(Double amount, String currency) throws RazorpayException;
}
