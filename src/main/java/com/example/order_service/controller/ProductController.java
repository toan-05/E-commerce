package com.example.order_service.controller;

import com.example.order_service.dto.request.product.CreateProductRequest;
import com.example.order_service.dto.request.product.UpdateProductRequest;
import com.example.order_service.dto.response.common.ApiResponse;
import com.example.order_service.dto.response.common.PageResponse;
import com.example.order_service.dto.response.product.ProductResponse;
import com.example.order_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for product catalog operations.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Creates a new product.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success("Product created", ProductResponse.from(productService.createProduct(request)));
    }

    /**
     * Returns one product by id.
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
        return ApiResponse.success("Product fetched", ProductResponse.from(productService.getProduct(id)));
    }

    /**
     * Returns all products.
     */
    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getProducts(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                "Products fetched",
                PageResponse.from(productService.getProducts(pageable), ProductResponse::from)
        );
    }

    /**
     * Replaces product details.
     */
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.success("Product updated", ProductResponse.from(productService.updateProduct(id, request)));
    }

    /**
     * Deletes one product by id.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success("Product deleted");
    }
}
