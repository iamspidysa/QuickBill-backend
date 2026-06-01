package com.saurabh.quickbill.controller;

import com.saurabh.quickbill.io.OrderRequest;
import com.saurabh.quickbill.io.OrderResponse;
import com.saurabh.quickbill.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request){
        return orderService.createOrder(request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{orderId}")
    public void deleteOrder(@PathVariable String orderId){
        orderService.deleteOrder(orderId);
    }

    // Before: returned List<OrderResponse> — loaded the entire table.
    // After:  returns Page<OrderResponse> with ?page=0&size=20 query params.
    //
    // defaultValue="0"  → first page if not specified
    // defaultValue="20" → 20 orders per page if not specified
    //
    // The Page<> response body looks like:
    // {
    //   "content": [ ...orders... ],
    //   "totalElements": 342,
    //   "totalPages": 18,
    //   "number": 0,          ← current page
    //   "size": 20,
    //   "last": false         ← whether this is the last page
    // }
    @GetMapping("/latest")
    public Page<OrderResponse> getLatestOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // Guard against clients requesting absurdly large pages
        int safeSize = Math.min(size, 100);

        return orderService.getLatestOrders(page, safeSize);
    }
}
