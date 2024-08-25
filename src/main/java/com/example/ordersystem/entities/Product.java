package com.example.ordersystem.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @NotNull
    private String productCode;
    @NotNull
    private String productName;
    @NotNull
    private int stock;
    @NotNull
    private double price;

    // Getters and Setters

}
