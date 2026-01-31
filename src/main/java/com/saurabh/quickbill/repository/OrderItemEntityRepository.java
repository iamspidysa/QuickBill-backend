package com.saurabh.quickbill.repository;

import com.saurabh.quickbill.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrderItemEntityRepository extends JpaRepository<OrderItemEntity,Long> {


}
