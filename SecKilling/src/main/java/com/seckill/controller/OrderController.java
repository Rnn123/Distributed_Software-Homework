package com.seckill.controller;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.entity.Order;
import com.seckill.entity.PaymentRecord;
import com.seckill.entity.User;
import com.seckill.service.OrderService;
import com.seckill.service.PaymentService;
import com.seckill.service.SeckillOrderProcessor;
import com.seckill.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final UserService userService;
    private final SeckillOrderProcessor orderProcessor;

    public OrderController(OrderService orderService,
                           PaymentService paymentService,
                           UserService userService,
                           SeckillOrderProcessor orderProcessor) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.userService = userService;
        this.orderProcessor = orderProcessor;
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

    @PostMapping("/{orderId}/pay")
    public Result<String> pay(@PathVariable Long orderId,
                              @RequestHeader(value = "Authorization", required = false) String authorization,
                              @RequestHeader(value = "X-Auth-Token", required = false) String tokenHeader) {
        User user = userService.getUserByToken(resolveToken(authorization, tokenHeader));
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        Order order = orderService.getById(orderId);
        if (order == null) {
            return Result.error(CodeMsg.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(user.getId())) {
            return Result.error(CodeMsg.ORDER_NOT_BELONG_TO_USER);
        }
        if (order.getStatus() == OrderService.STATUS_PAID) {
            return Result.error(CodeMsg.PAYMENT_DUPLICATE);
        }
        if (order.getStatus() != OrderService.STATUS_UNPAID) {
            return Result.error(CodeMsg.ORDER_STATUS_ERROR);
        }

        PaymentRecord paymentRecord = paymentService.pay(order);
        orderProcessor.markPaying(orderId);
        return Result.success("PAYMENT_ACCEPTED:" + paymentRecord.getId());
    }

    private String resolveToken(String authorization, String tokenHeader) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return tokenHeader;
    }
}
