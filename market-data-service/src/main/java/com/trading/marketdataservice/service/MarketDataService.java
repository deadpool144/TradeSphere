package com.trading.marketdataservice.service;


import com.trading.marketdataservice.dto.StockPrice;
import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final StockRepository stockRepository;
    private final Random random=new Random();
    private final RedisTemplate<String, Object> redisTemplate;
    private  final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String,Object> kafkaTemplate;


    private static final String PRICE_KEY_PREFIX="stocks:price:";
    private static final String PRICE_UPDATED_TOPIC="stocks.price.updated";


    private final Map<String, StockPrice> stockPrices=new ConcurrentHashMap<>(); // conurrentHashMap for thread safty

    /**
     * Initialize stocks prices
     *  postConstruct - runs  when context is ready
     */

    @PostConstruct
    public void initializePrices(){

        log.info("loading stock data...");

        List<Stock> stocks=stockRepository.findByActiveTrue();

        if(stocks.isEmpty()){
            log.warn("no active  stocks found");
            return;
        }
        stocks.forEach(stock->{

            StockPrice stockPrice=new StockPrice();
            stockPrice.setSymbol(stock.getSymbol());
            stockPrice.setCompanyName(stock.getCompanyName());
            stockPrice.setPrice(stock.getInitialPrice());
            stockPrice.setOpen(stock.getInitialPrice());
            stockPrice.setHigh(stock.getInitialPrice());
            stockPrice.setLow(stock.getInitialPrice());
            stockPrice.setChange(BigDecimal.ZERO);
            stockPrice.setChangePercent(BigDecimal.ZERO);
            stockPrice.setVolume(0L);
            stockPrice.setTimestamp(LocalDateTime.now());

            stockPrices.put(stock.getSymbol(),stockPrice);

            log.info("loaded stock {} at {}",stock.getSymbol(),stock.getInitialPrice());
        });

        log.info("Initialized {} stocks",stockPrices.size());
    }


    /**
     * Scheduled price update every 2s
     */

    @Scheduled(fixedRateString=
            "${market.price-update-interval}")
    public void updatePrices(){

        if(stockPrices.isEmpty()){
            log.warn("no active  stocks loaded");
            return;
        }

        stockPrices.forEach((stockSymbol, currentPrice) ->{
            BigDecimal oldPrice=currentPrice.getPrice();

            double priceMovement=(random.nextDouble()-0.5*0.5);

            BigDecimal priceChange= oldPrice
                    .multiply(BigDecimal
                            .valueOf(priceMovement/100))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal newPrice=oldPrice
                    .add(priceChange)
                    .setScale(2, RoundingMode.HALF_UP);

            // prevent -ve price

            if(newPrice.compareTo(oldPrice)<=0){
                newPrice=BigDecimal.valueOf(1);

            }

            BigDecimal changeAmount=newPrice
                    .subtract(currentPrice.getOpen())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal changePercent = changeAmount
                    .divide(currentPrice.getOpen(),
                            RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            //update high / low

            BigDecimal newHigh =newPrice
                    .compareTo(currentPrice.getHigh())>0
                    ?newPrice:currentPrice.getHigh();

            BigDecimal newLow =newPrice.
                    compareTo(currentPrice.getLow())<0
                    ?newPrice:currentPrice.getLow();


            currentPrice.setPrice(newPrice);
            currentPrice.setChange(changeAmount);
            currentPrice.setChangePercent(changePercent);
            currentPrice.setHigh(newHigh);
            currentPrice.setLow(newLow);
            currentPrice.setTimestamp(LocalDateTime.now());


            // 1. cache in redis
            redisTemplate.opsForValue().set(
                    PRICE_KEY_PREFIX+stockSymbol,currentPrice
            );

            // brodcast via websocket - all clients
            messagingTemplate.convertAndSend(
                    "/topic/prices" + stockSymbol, currentPrice
            );

            //publish to kafka
            Map<String,Object> priceUpdatedEvent=new HashMap<>();
            priceUpdatedEvent.put("symbol",stockSymbol);
            priceUpdatedEvent.put("price",newPrice);
            priceUpdatedEvent.put("timestamp",LocalDateTime
                    .now()
                    .toString());

            kafkaTemplate.send(PRICE_UPDATED_TOPIC,stockSymbol,priceUpdatedEvent);

        });
        log.debug("Updated {} stocks",stockPrices.size());

    }

    /**
     * get price from redis
     */
    public StockPrice getPrice(String symbol){
        Object cached=redisTemplate.opsForValue()
                .get(PRICE_KEY_PREFIX+symbol);

        if(cached!=null){
            return (StockPrice) cached;
        }
        //fallback
        StockPrice price=stockPrices
                .get(symbol
                        .toUpperCase());
        if(price==null){
            throw new RuntimeException("no stock found for symbol "+symbol);
        }
        return price;
    }

    //getAllprices -gets all active stck prices

    public List<StockPrice> getAllPrices(){
        return stockPrices.values().stream().toList();
    }

    // get all stocks
    public List<Stock> getAllStocks(){
        return stockRepository.findByActiveTrue();
    }
}
