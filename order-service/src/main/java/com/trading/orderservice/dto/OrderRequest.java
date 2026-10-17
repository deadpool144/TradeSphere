package com.trading.orderservice.dto;


import com.trading.orderservice.entity.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private String userId;

    @NotBlank(message ="Stocks symbol is required")
    private String symbol;

    @NotBlank(message = "Order types is required BUY or SELL")
    private OrderType type;

    @Min(value = 1,message = "Quantity must be min 1")
    private int quantity;




}
