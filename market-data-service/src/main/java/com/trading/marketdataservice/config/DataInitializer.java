package com.trading.marketdataservice.config;

import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@Order(value = 1)
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;


    @Override
    public void run(String... args) throws Exception {
        if(stockRepository.count()>0){
            log.info("Stock Data Service has been initialized - skipping");
            return;
        }

        log.info("initializing stock data...");

        List<Stock> stocks = List.of(

                // US Stocks (traded on NASDAQ in USD)
                new Stock("AAPL", "Apple Inc", BigDecimal.valueOf(342.33), "NASDAQ", "USD", true),
                new Stock("GOOGL", "Alphabet Inc", BigDecimal.valueOf(343.01), "NASDAQ", "USD", true),
                new Stock("MSFT", "Microsoft Corp", BigDecimal.valueOf(531.43), "NASDAQ", "USD", true),

                // Indian Stocks (traded on NSE in INR)
                new Stock("RELIANCE", "Reliance Industries Ltd", BigDecimal.valueOf(1202.00), "NSE", "INR", true),
                new Stock("TCS", "Tata Consultancy Services Ltd", BigDecimal.valueOf(2460.90), "NSE", "INR", true),
                new Stock("INFY", "Infosys Ltd", BigDecimal.valueOf(1130.70), "NSE", "INR", true)
        );

        stockRepository.saveAll(stocks);
        log.info("stock data initialized");
    }
}
