package com.seckill.controller;

import com.seckill.common.CodeMsg;
import com.seckill.common.Result;
import com.seckill.entity.Product;
import com.seckill.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    ProductService productService;

    @GetMapping("/detail/{productId}")
    public Result<Product> detail(@PathVariable("productId") long productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return Result.error(new CodeMsg(404, "Product not found"));
        }
        return Result.success(product);
    }
}
