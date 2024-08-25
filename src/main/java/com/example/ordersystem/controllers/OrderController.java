package com.example.ordersystem.controllers;

import com.example.ordersystem.entities.Order;
import com.example.ordersystem.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(@RequestParam Long userId, @RequestParam String productCode, @RequestParam int quantity) {
        Order order = orderService.createOrder(userId, productCode, quantity);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/pay/{orderNumber}")
    public ResponseEntity<Order> payOrder(@PathVariable String orderNumber) {
        Order order = orderService.payOrder(orderNumber);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<Order> getOrderInfo(@PathVariable String orderNumber) {
        Order order = orderService.getOrderInfo(orderNumber);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> batchProcessOrderCancellation() {
        orderService.batchProcessOrderCancellation();
        return ResponseEntity.ok().build();
    }
}
