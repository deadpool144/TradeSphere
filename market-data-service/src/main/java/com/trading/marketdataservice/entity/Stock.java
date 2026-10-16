package com.trading.marketdataservice.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Entity
@Table(name="stocks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Stock {

    @Id
    @Column(nullable=false)
    private  String symbol;

    @Column(nullable=false)
    private  String companyName;

    @Column(nullable=false, precision = 15, scale=2)
    private BigDecimal initialPrice;

    @Column(nullable=false)
    private String exchange; //nse...
    @Column(nullable=false)
    private String currency; //inr...
    @Column(nullable=false)
    private boolean active=true;


}
