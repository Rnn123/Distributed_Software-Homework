package com.seckill.controller;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.entity.Order;
import com.seckill.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public Result<Order> detail(@PathVariable Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            return Result.error(CodeMsg.ORDER_NOT_FOUND);
        }
        return Result.success(order);
    }

    @GetMapping
    public Result<List<Order>> listByUser(@RequestParam Long userId) {
        return Result.success(orderService.listByUserId(userId));
    }
}
