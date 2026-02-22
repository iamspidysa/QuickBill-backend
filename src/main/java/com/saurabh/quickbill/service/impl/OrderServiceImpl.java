package com.saurabh.quickbill.service.impl;

import com.saurabh.quickbill.entity.OrderEntity;
import com.saurabh.quickbill.entity.OrderItemEntity;
import com.saurabh.quickbill.io.*;
import com.saurabh.quickbill.repository.OrderEntityRepository;
import com.saurabh.quickbill.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderEntityRepository orderEntityRepository;

    @Override
    public OrderResponse createOrder(OrderRequest request) {
         OrderEntity newOrder = convertToOrderEntity(request);

        PaymentDetails paymentDetails = new PaymentDetails();
        paymentDetails.setStatus(newOrder.getPaymentMethod() == PaymentMethod.CASH ?
                PaymentDetails.PaymentStatus.COMPLETED : PaymentDetails.PaymentStatus.PENDING);
        newOrder.setPaymentDetails(paymentDetails);

        List<OrderItemEntity> orderItems = request.getCartItems().stream()
                .map(this::convertToOrderItemEntity)
                .collect(Collectors.toList());
        newOrder.setItems(orderItems);

        newOrder = orderEntityRepository.save(newOrder);
        return convertToResponse(newOrder);

    }

    private OrderItemEntity convertToOrderItemEntity(OrderRequest.OrderItemRequest orderItemRequest) {
        return OrderItemEntity.builder()
                .itemId(orderItemRequest.getItemId())
                .name(orderItemRequest.getName())
                .price(orderItemRequest.getPrice())
                .quantity(orderItemRequest.getQuantity())
                .build();
    }

    private OrderResponse convertToResponse(OrderEntity newOrder) {
        return OrderResponse.builder()
                .orderId(newOrder.getOrderId())
                .customerName(newOrder.getCustomerName())
                .phoneNumber(newOrder.getPhoneNumber())
                .subTotal(newOrder.getSubTotal())
                .tax(newOrder.getTax())
                .grandTotal(newOrder.getGrandTotal())
                .paymentMethod(newOrder.getPaymentMethod())
                .items(newOrder.getItems().stream()
                        .map(this::convertToItemResponse)
                        .collect(Collectors.toList()))
                .paymentDetails(newOrder.getPaymentDetails())
                .createdAt(newOrder.getCreatedAt())
                .build();


    }

    private OrderResponse.OrderItemResponse convertToItemResponse(OrderItemEntity orderItemEntity) {
        return OrderResponse.OrderItemResponse.builder()
                .itemId(orderItemEntity.getItemId())
                .name(orderItemEntity.getName())
                .price(orderItemEntity.getPrice())
                .quantity(orderItemEntity.getQuantity())
                .build();

    }

    private OrderEntity convertToOrderEntity(OrderRequest request) {
        return OrderEntity.builder()
                .customerName(request.getCustomerName())
                .phoneNumber(request.getPhoneNumber())
                .subTotal(request.getSubTotal())
                .tax(request.getTax())
                .grandTotal(request.getGrandTotal())
                .paymentMethod(PaymentMethod.
                        valueOf(request.getPaymentMethod()))
                .build();
    }

    @Override
    public void deleteOrder(String orderId) {
        OrderEntity existingOrder = orderEntityRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        orderEntityRepository.delete(existingOrder);
    }

    @Override
    public List<OrderResponse> getLatestOrders() {
        return orderEntityRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse verifyPayment(PaymentVerificationRequest request) {
            OrderEntity order = orderEntityRepository.findByOrderId(request.getOrderId())
                    .orElseThrow( ()-> new RuntimeException("Order not found"));

            if(!verifyRazorpaySignature(request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature())){
                throw new RuntimeException("Payment Verification failed");
            }

            PaymentDetails paymentDetails = order.getPaymentDetails();
            paymentDetails.setRazorpayOrderId(request.getRazorpayOrderId());
            paymentDetails.setRazorpayPaymentId(request.getRazorpayPaymentId());
            paymentDetails.setRazorpaySignature(request.getRazorpaySignature());
            paymentDetails.setStatus(PaymentDetails.PaymentStatus.COMPLETED);

            order = orderEntityRepository.save(order);
            return convertToResponse(order);
    }

    private boolean verifyRazorpaySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        // In Production grade , we need to verify signature not just return true.
        return true;
    }
}
