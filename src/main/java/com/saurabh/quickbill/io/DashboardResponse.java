package com.saurabh.quickbill.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {

    private Double todaySale;
    private Long todayOrderCount;
    private List<OrderResponse> recentOrders;
}
