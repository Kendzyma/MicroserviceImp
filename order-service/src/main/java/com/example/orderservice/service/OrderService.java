package com.example.orderservice.service;

import com.example.orderservice.dto.InventoryResponse;
import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final WebClient.Builder webClientBuilder;
    private final OrderRepository orderRepository;
    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

       List<OrderLineItems> orderLineItemsList =  orderRequest.getOrderLineItemsList()
                .stream()
                .map(this::mapToDto).toList();
       order.setOrderLineItemsList(orderLineItemsList);
List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();
       //call inventory service and place order if product is in stock
    InventoryResponse[] inventoryResponses = webClientBuilder.build().get().uri("http://inventory-service/api/inventory",uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

    boolean allProductsInstock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

    if (allProductsInstock){
        orderRepository.save(order);
    }
    else {
        throw new IllegalArgumentException("Product not in stock");
    }
    return "Order placed successfully";
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        return orderLineItems;
    }
}
