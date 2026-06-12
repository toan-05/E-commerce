package com.example.order_service.service.impl;

import com.example.order_service.entity.Product;
import com.example.order_service.entity.enums.RecordStatus;
import com.example.order_service.dto.request.product.CreateProductRequest;
import com.example.order_service.dto.request.product.UpdateProductRequest;
import com.example.order_service.exception.ErrorCode;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.repository.ProductRepository;
import com.example.order_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Product createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .build();

        return productRepository.save(product);
    }

    @Override
    public Product getProduct(Long id) {
        return productRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product", id));
    }

    @Override
    public Page<Product> getProducts(Pageable pageable) {
        return productRepository.findAllByStatus(RecordStatus.ACTIVE, pageable);
    }

    @Override
    public Product updateProduct(Long id, UpdateProductRequest request) {
        Product product = getProduct(id);
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        product.markDeleted();
        productRepository.save(product);
    }
}
