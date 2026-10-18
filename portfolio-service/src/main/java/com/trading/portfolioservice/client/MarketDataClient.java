package com.trading.portfolioservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient
public interface MarketDataClient {

    @GetMapping("/api/v1/market/stocks/{symbol}")
    Map<String,Object> getStockPrice(@PathVariable String symbol);
}
