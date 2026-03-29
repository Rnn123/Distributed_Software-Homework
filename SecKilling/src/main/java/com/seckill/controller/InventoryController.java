package com.seckill.controller;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.entity.Inventory;
import com.seckill.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public Result<Inventory> detail(@PathVariable Long productId) {
        Inventory inventory = inventoryService.getByProductId(productId);
        if (inventory == null) {
            return Result.error(CodeMsg.STOCK_NOT_FOUND);
        }
        return Result.success(inventory);
    }
}
