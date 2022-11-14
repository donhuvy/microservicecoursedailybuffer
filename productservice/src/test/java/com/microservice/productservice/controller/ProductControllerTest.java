package com.microservice.productservice.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.microservice.productservice.entity.Product;
import com.microservice.productservice.payload.request.ProductRequest;
import com.microservice.productservice.payload.response.ProductResponse;
import com.microservice.productservice.repository.ProductRepository;
import com.microservice.productservice.service.ProductService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest({"server.port=0"})
@AutoConfigureMockMvc
@EnableConfigurationProperties
@ActiveProfiles
public class ProductControllerTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MockMvc mockMvc;

    @RegisterExtension
    static WireMockExtension wireMockServer
            = WireMockExtension.newInstance()
            .options(
                    WireMockConfiguration
                            .wireMockConfig()
                            .port(8080)
            )
            .build();

    private ObjectMapper objectMapper
            = new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void test_WhenaddProduct_isSuccess() throws Exception {
        ProductRequest productRequest = getMockProductRequest();

        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.post("/product")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        String productId = mvcResult.getResponse().getContentAsString();

        Optional<Product> product = productRepository.findById(Long.valueOf(productId));
        assertTrue(product.isPresent());

        Product p = product.get();

        assertEquals(Long.parseLong(productId), p.getProductId());
        assertEquals(productRequest.getQuantity(), p.getQuantity());
        assertEquals(productRequest.getName(), p.getProductName());
        assertEquals(productRequest.getPrice(), p.getPrice());

    }

    @Test
    @Disabled
    void test_When_addProduct_WithWrongAccess_thenThrow_403() throws Exception {
        ProductRequest productRequest = getMockProductRequest();

        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.post("/product")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andReturn();
    }

    @Test
    void test_When_getProductById_isSuccess() throws Exception {
        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.get("/product/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String actualResponse = mvcResult.getResponse().getContentAsString();
        Product product = productRepository.findById(1l).get();
        String expectedResponse = getMockProductResponse(product);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void test_When_getProductById_isNotFound() throws Exception {
        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.get("/product/2")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();
    }

    @Test
    void test_When_reduceQuantity_isSuccess() throws Exception {


        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.put("/product/reduceQuantity/1")
                        .param("quantity", String.valueOf(1))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

    }

    @Test
    void test_When_reduceQuantity_isFailed_for_productId_isNotFound() throws Exception {


        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.put("/products/reduceQuantity/2")
                        .param("quantity", String.valueOf(1))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();

    }

    @Test
    void test_When_reduceQuantity_isFailed_for_inSufficient_quantity() throws Exception {


        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.put("/products/reduceQuantity/1")
                        .param("quantity", String.valueOf(11))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();

    }

    @Test
    void test_When_deleteProductById_isSuccess() throws Exception {

        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.delete("/product/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    void test_when_deleteProductById_when_productId_isNotFound() throws Exception {

        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.delete("/product/2")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();
    }

    @Test
    @Disabled
    void test_when_deleteProductById_WithWrongAccess_thenThrow_403() throws Exception {

        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.delete("/product/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andReturn();
    }


    private ProductRequest getMockProductRequest() {
        return ProductRequest.builder()
                .name("iphone")
                .price(1000)
                .quantity(10)
                .build();
    }


    private String getMockProductResponse(Product product) throws IOException {

        ProductResponse productResponse
                = ProductResponse.builder()
                .productName(product.getProductName())
                .quantity(product.getQuantity())
                .productId(product.getProductId())
                .price(product.getPrice())
                .build();

        return objectMapper.writeValueAsString(productResponse);
    }
}
