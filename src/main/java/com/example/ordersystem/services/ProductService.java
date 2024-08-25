package com.example.ordersystem.services;

import com.example.ordersystem.entities.Product;
import com.example.ordersystem.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProductService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductRepository productRepository;


    // Get product from cache or database
    public Product getProduct(String productCode) {
        Product product = (Product) redisTemplate.opsForValue().get("product:" + productCode);
        if (product == null) {
            Optional<Product> optionalProduct = productRepository.findByProductCode(productCode);
            if (optionalProduct.isPresent()) {
                product = optionalProduct.get();
                redisTemplate.opsForValue().set("product:" + product.getProductCode(), product);
            }
        }
        return product;
    }

    // Create a new product and update cache
    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        redisTemplate.opsForValue().set("product:" + savedProduct.getProductCode(), savedProduct);
        return savedProduct;
    }

    // Retrieve a product by ID from cache, if not found then get from db and update cache
    public Optional<Product> getProductById(Long id) {
        String cacheKey = "product:" + id;
        Product cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            return Optional.of(cachedProduct);
        }
        Optional<Product> product = productRepository.findById(id);
        product.ifPresent(value -> redisTemplate.opsForValue().set(cacheKey, value));
        return product;
    }

    // Retrieve all products
    public List<Product> getAllProducts() {
        // Step 1: Get all keys from Redis that match the pattern "product:*"
        Set<String> productKeys = redisTemplate.keys("product:*");

        List<Product> products = new ArrayList<>();

        // Step 2: Check if there are any products in the cache
        if (productKeys != null && !productKeys.isEmpty()) {
            // Step 3: Retrieve all cached products
            List<Object> cachedProducts = redisTemplate.opsForValue().multiGet(productKeys);
            List<Product> finalProducts = products;
            cachedProducts.forEach(product -> finalProducts.add((Product) product));
        }

        // Step 4: If any products were missing from the cache, fetch from the database
        if (products.size() < productRepository.count()) {
            List<Product> dbProducts = productRepository.findAll();

            // Step 5: Update the cache with products retrieved from the database
            dbProducts.forEach(product -> {
                if (!productKeys.contains("product:" + product.getProductCode())) {
                    redisTemplate.opsForValue().set("product:" + product.getProductCode(), product);
                }
            });

            products = dbProducts;
        }

        return products;
    }


    // Update an existing product and update cache
    public Product updateProduct(Long id, Product updatedProduct) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setProductCode(updatedProduct.getProductCode());
                    product.setProductName(updatedProduct.getProductName());
                    product.setStock(updatedProduct.getStock());
                    product.setPrice(updatedProduct.getPrice());
                    Product savedProduct = productRepository.save(product);
                    redisTemplate.opsForValue().set("product:" + savedProduct.getProductCode(), savedProduct);
                    return savedProduct;
                }).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    // Delete a product and remove it from cache
    public void deleteProduct(Long id) {
        productRepository.findById(id).ifPresent(product -> {
            redisTemplate.delete("product:" + product.getProductCode());
            productRepository.delete(product);
        });
    }

    private List<String> getProductKeys() {
        List<Product> allProducts = productRepository.findAll();
        List<String> keys = new ArrayList<>();
        allProducts.forEach(product -> keys.add("product:" + product.getProductCode()));
        return keys;
    }

    // add quantity to product in db and redis
    public Product addQuantity(String productCode, int quantity) {
        Product product = (Product) redisTemplate.opsForValue().get("product:" + productCode);

        if (product == null) {
            product = productRepository.findByProductCode(productCode)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
        }

        product.setStock(product.getStock() + quantity);

        // 更新数据库
        productRepository.save(product);

        // 更新缓存
        redisTemplate.opsForValue().set("product:" + product.getProductCode(), product);

        return product;
    }

    // deduct quantity in db and redis
    public Product deductQuantity(String productCode, int quantity) {
        Product product = (Product) redisTemplate.opsForValue().get("product:" + productCode);

        if (product == null) {
            product = productRepository.findByProductCode(productCode)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        product.setStock(product.getStock() - quantity);

        // 更新数据库
        productRepository.save(product);

        // 更新缓存
        redisTemplate.opsForValue().set("product:" + product.getProductCode(), product);

        return product;
    }

}
