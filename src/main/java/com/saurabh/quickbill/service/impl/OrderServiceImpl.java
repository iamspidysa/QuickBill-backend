package com.saurabh.quickbill.service.impl;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.saurabh.quickbill.entity.ItemEntity;
import com.saurabh.quickbill.entity.OrderEntity;
import com.saurabh.quickbill.entity.OrderItemEntity;
import com.saurabh.quickbill.exception.PaymentVerificationException;
import com.saurabh.quickbill.exception.ResourceNotFoundException;
import com.saurabh.quickbill.io.*;
import com.saurabh.quickbill.repository.ItemRepository;
import com.saurabh.quickbill.repository.OrderEntityRepository;
import com.saurabh.quickbill.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    /**
     * Tax rate applied to every order.
     * Defaults to 18% (standard Indian GST for most restaurant/retail items).
     * Override in application-prod.properties with app.order.tax-rate=0.xx
     */
    @Value("${app.order.tax-rate:0.18}")
    private BigDecimal taxRate;

    private final OrderEntityRepository orderEntityRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        // ── 1. Resolve each cart item against the DB ──────────────────────
        // Prices and names come from tbl_items, never from the request.
        // Any unknown itemId fails fast with 404 before we touch the DB.
        List<OrderItemEntity> orderItems = request.getCartItems().stream()
                .map(cartItem -> {
                    ItemEntity item = itemRepository.findByItemId(cartItem.getItemId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Item not found: " + cartItem.getItemId()));

                    return OrderItemEntity.builder()
                            .itemId(item.getItemId())
                            .name(item.getName())
                            .price(item.getPrice())           // DB price — never client price
                            .quantity(cartItem.getQuantity())
                            .build();
                })
                .toList();

        // ── 2. Compute totals server-side ────────────────────────────────
        BigDecimal subTotal = orderItems.stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal tax = subTotal
                .multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grandTotal = subTotal.add(tax);

        // ── 3. Build and persist the order ───────────────────────────────
        PaymentDetails paymentDetails = new PaymentDetails();
        paymentDetails.setStatus(
                PaymentMethod.valueOf(request.getPaymentMethod()) == PaymentMethod.CASH
                        ? PaymentDetails.PaymentStatus.COMPLETED
                        : PaymentDetails.PaymentStatus.PENDING);

        OrderEntity newOrder = OrderEntity.builder()
                .customerName(request.getCustomerName())
                .phoneNumber(request.getPhoneNumber())
                .subTotal(subTotal)
                .tax(tax)
                .grandTotal(grandTotal)
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .paymentDetails(paymentDetails)
                .items(orderItems)
                .build();

        newOrder = orderEntityRepository.save(newOrder);
        return convertToResponse(newOrder);
    }

    @Override
    public void deleteOrder(String orderId) {
        OrderEntity existingOrder = orderEntityRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        orderEntityRepository.delete(existingOrder);
    }

    @Override
    public List<OrderResponse> getLatestOrders() {
        return orderEntityRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse verifyPayment(PaymentVerificationRequest request) {
        OrderEntity order = orderEntityRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        PaymentDetails paymentDetails = order.getPaymentDetails();
        paymentDetails.setRazorpayOrderId(request.getRazorpayOrderId());
        paymentDetails.setRazorpayPaymentId(request.getRazorpayPaymentId());
        paymentDetails.setRazorpaySignature(request.getRazorpaySignature());
        paymentDetails.setStatus(PaymentDetails.PaymentStatus.COMPLETED);

        order = orderEntityRepository.save(order);
        return convertToResponse(order);
    }

    @Override
    public OrderResponse getOrderById(String orderId) {
        OrderEntity order = orderEntityRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return convertToResponse(order);
    }

    @Override
    public Double sumSalesByDate(LocalDate date) {
        return orderEntityRepository.sumSalesByDate(date);
    }

    @Override
    public Long countByOrderDate(LocalDate date) {
        return orderEntityRepository.countByOrderDate(date);
    }

    @Override
    public List<OrderResponse> findRecentOrders() {
        return orderEntityRepository.findRecentOrders(PageRequest.of(0, 5))
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private OrderResponse convertToResponse(OrderEntity order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .customerName(order.getCustomerName())
                .phoneNumber(order.getPhoneNumber())
                .subTotal(order.getSubTotal())
                .tax(order.getTax())
                .grandTotal(order.getGrandTotal())
                .paymentMethod(order.getPaymentMethod())
                .items(order.getItems().stream()
                        .map(this::convertToItemResponse)
                        .toList())
                .paymentDetails(order.getPaymentDetails())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderResponse.OrderItemResponse convertToItemResponse(OrderItemEntity item) {
        return OrderResponse.OrderItemResponse.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .build();
    }

    private void verifyRazorpaySignature(String razorpayOrderId,
                                         String razorpayPaymentId,
                                         String razorpaySignature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);
            Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
        } catch (RazorpayException e) {
            throw new PaymentVerificationException(
                    "Payment signature verification failed. Possible fraud attempt.");
        }
    }
}