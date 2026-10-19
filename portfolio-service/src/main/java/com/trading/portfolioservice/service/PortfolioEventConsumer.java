package com.trading.portfolioservice.service;


import com.trading.portfolioservice.entity.Holding;
import com.trading.portfolioservice.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.Holder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioEventConsumer {


    private  final HoldingRepository holdingRepository;

    @KafkaListener(topics = "order.executed")
    public void consumeOrderEventExecuted(
            @Payload Map<String,Object> payload){

        try {
            String userId = (String) payload.get("userId");
            String symbol = (String) payload.get("symbol");
            String type = (String) payload.get("type");

            int quantity = Integer.parseInt((String) payload.get("quantity").toString());
            BigDecimal price = new  BigDecimal((String)payload.get("price").toString());

            log.info("Updating portfolio - user : {} {} x {} at {}",
                    userId, symbol, quantity, price);

            if("Buy".equals(type)){
                addHoldings(userId,symbol,quantity,price);
            }
            else  if("SELL".equals(type)){
                removeHoldings(userId,symbol,quantity);
            }


        }catch (Exception e){
            log.error("Error updating portfolio - user : {}",e.getMessage());

        }
    }

    /**
     * add or update after buy order
     * cal new avg buy price
     * @param userId
     * @param symbol
     * @param quantity
     * @param price
     */

    private void addHoldings(String userId, String symbol, int quantity, BigDecimal price){
        Holding holding = holdingRepository
                .findByUserIdAndSymbol(userId,symbol)
                .orElse(null);

        if(holding==null){
            //first time buying this stock
            holding = new Holding();
            holding.setUserId(userId);
            holding.setSymbol(symbol);
            holding.setCompanyName(symbol);
            holding.setQuantity(quantity);
            holding.setAverageBuyPrice(price);
            holding.setTotalInvested(
                    price.multiply(BigDecimal.valueOf(quantity)));

        }
        else {
            //already holding this stock - update avg price

            int newQuantity = quantity + holding.getQuantity();
            BigDecimal newInvested = holding.getTotalInvested()
                    .add(price.multiply(BigDecimal.valueOf(newQuantity)));
            BigDecimal newAverageBuyPrice = newInvested
                    .divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);

            holding.setQuantity(newQuantity);
            holding.setTotalInvested(newInvested);
            holding.setAverageBuyPrice(newAverageBuyPrice);

            holdingRepository.save(holding);

            log.info("Holding updated : {} {} shares of {}",
                    userId, holding.getQuantity(),symbol);


        }

    }


    private void removeHoldings(String userId, String symbol, int quantity){

        Holding holding = holdingRepository
                .findByUserIdAndSymbol(userId,symbol)
                .orElseThrow(()->new RuntimeException("Holding not found"+symbol));

        if(holding.getQuantity()<quantity){
            throw new RuntimeException("Insufficient shares to SELL");

        }

        int newQuantity = holding.getQuantity() - quantity;

        if(newQuantity==0){
            holdingRepository.delete(holding);
            log.info("Holding removed : {} sold all {} shares",
                    userId,symbol);
        }
        else {
            BigDecimal newInvested = holding.getTotalInvested()
                    .multiply(BigDecimal.valueOf(newQuantity));
            holding.setQuantity(newQuantity);
            holding.setTotalInvested(newInvested);

            holdingRepository.save(holding);

            log.info("Holding reduced: {} now ahs {} shared of {}",
                    userId, holding.getQuantity(),symbol);
        }

    }




}
