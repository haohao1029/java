package com.example.ordersystem.controllers;

import com.example.ordersystem.entities.Order;
import com.example.ordersystem.entities.Product;
import com.example.ordersystem.repositories.OrderRepository;
import com.example.ordersystem.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(@RequestParam String productCode, @RequestParam int quantity) {
        Order order = orderService.createOrder(productCode, quantity);
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

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrder() {
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }
}
