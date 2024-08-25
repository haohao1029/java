package com.example.ordersystem.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    @Column(unique = true)
    private String orderNumber;
    private String productCode;
    private int quantity;
    private double amount;
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private LocalDateTime createdTime;

    // Getters and Setters

}
