package com.example.ordersystem.services;

import com.example.ordersystem.entities.Order;
import com.example.ordersystem.entities.OrderStatus;
import com.example.ordersystem.entities.Product;
import com.example.ordersystem.repositories.OrderRepository;
import com.example.ordersystem.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private ProductService productService;


    @Transactional
    public Order createOrder(Long userId, String productCode, int quantity) {
        String lockKey = "lock:product:" + productCode;
        String lockValue = UUID.randomUUID().toString();

        // 尝试获取商品的分布式锁，锁定时间10秒
        boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);

        if (!lockAcquired) {
            throw new RuntimeException("Could not acquire lock, please try again");
        }

        try {
            Product product = productService.getProduct(productCode);
            if (product.getStock() < quantity) {
                throw new RuntimeException("Insufficient stock");
            }

            // 扣减库存
            productService.deductQuantity(productCode, quantity);

            // 创建订单
            Order order = new Order();
            order.setUserId(userId);
            order.setProductCode(productCode);
            order.setQuantity(quantity);
            order.setAmount(product.getPrice() * quantity);
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedTime(LocalDateTime.now());

            orderRepository.save(order);

            return order;

        } finally {
            // 确保锁的释放，只在当前持有锁的进程才释放锁
            String currentLockValue = (String) redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }


    @Transactional
    public Order payOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be paid");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // delete redis
        redisTemplate.delete("order" + order.getId());

        // Send order paid message to RocketMQ
        rocketMQTemplate.convertAndSend("order-topic", "Paid Order: " + order.getId());


        return order;
    }

    public Order getOrderInfo(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    // batch process cancel order
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional
    public void batchProcessOrderCancellation() {
        Set<String> keySet = redisTemplate.keys("order" + "*");
        if (keySet != null) {
            List<String> keys = new ArrayList<>(keySet);
            for (String key : keys) {
                Order order = (Order) redisTemplate.opsForValue().get(key);

                if (order == null || !order.getCreatedTime().plusMinutes(30).isBefore(LocalDateTime.now())) {
                    continue;
                }

                String productLockKey = "lock:product:" + order.getProductCode();
                String lockValue = UUID.randomUUID().toString();

                // Try to acquire the lock for the product with a timeout of 10 seconds
                boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(productLockKey, lockValue, 10, TimeUnit.SECONDS);

                if (!lockAcquired) {
                    // If the lock was not acquired, skip processing this order
                    continue;
                }

                try {
                    // Cancel the order
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    // Add back the stock quantity
                    productService.addQuantity(order.getProductCode(), order.getQuantity());

                    // Send order cancellation message to RocketMQ
                    rocketMQTemplate.convertAndSend("order-topic", "Cancelled Order: " + order.getId());

                    // Remove the order from Redis
                    redisTemplate.delete(key);
                } finally {
                    // Release the lock
                    // Only release if this process still holds the lock
                    String currentLockValue = (String) redisTemplate.opsForValue().get(productLockKey);
                    if (lockValue.equals(currentLockValue)) {
                        redisTemplate.delete(productLockKey);
                    }
                }
            }
        }
    }

}
