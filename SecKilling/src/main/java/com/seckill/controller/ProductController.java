package com.seckill.controller;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.entity.Product;
import com.seckill.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<List<Product>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(productService.searchProducts(keyword));
    }

    @GetMapping("/{productId}")
    public Result<Product> detail(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return Result.error(CodeMsg.PRODUCT_NOT_FOUND);
        }
        return Result.success(product);
    }
}
