package com.example.ordersystem.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @Column(unique = true)
    @NotNull
    private String orderNumber;
    @NotNull
    private String productCode;
    @NotNull
    private int quantity;
    @NotNull
    private double amount;
    @NotNull
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    @NotNull
    private OrderStatus status;
    @NotNull
    private LocalDateTime createdTime;

    // Getters and Setters

}
