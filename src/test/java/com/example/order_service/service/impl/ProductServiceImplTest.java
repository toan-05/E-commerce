package com.example.order_service.service.impl;

import com.example.order_service.dto.request.product.UpdateProductRequest;
import com.example.order_service.entity.Product;
import com.example.order_service.entity.enums.RecordStatus;
import com.example.order_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void updateProductReplacesProductDetails() {
        Product product = Product.builder()
                .id(1L)
                .name("Old")
                .price(BigDecimal.valueOf(10))
                .stockQuantity(1)
                .build();
        when(productRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        Product updated = productService.updateProduct(
                1L,
                new UpdateProductRequest("New", BigDecimal.valueOf(20), 5)
        );

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getPrice()).isEqualByComparingTo("20");
        assertThat(updated.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void deleteProductDeletesExistingProduct() {
        Product product = Product.builder()
                .id(1L)
                .name("Keyboard")
                .price(BigDecimal.valueOf(99))
                .stockQuantity(2)
                .build();
        when(productRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).save(product);
        assertThat(product.getStatus()).isEqualTo(RecordStatus.DELETED);
    }
}
