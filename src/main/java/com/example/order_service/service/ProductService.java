package com.example.order_service.service;

import com.example.order_service.dto.request.product.CreateProductRequest;
import com.example.order_service.dto.request.product.UpdateProductRequest;
import com.example.order_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Handles product catalog operations.
 */
public interface ProductService {

    /**
     * Creates a new product.
     */
    Product createProduct(CreateProductRequest request);

    /**
     * Returns one product by id.
     */
    Product getProduct(Long id);

    /**
     * Returns products by page.
     */
    Page<Product> getProducts(Pageable pageable);

    /**
     * Replaces product details.
     */
    Product updateProduct(Long id, UpdateProductRequest request);

    /**
     * Deletes a product by id.
     */
    void deleteProduct(Long id);
}
