package com.trading.orderservice.repository;

import com.trading.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public class OrderRepository extends JpaRepository<Order, String> {
}
