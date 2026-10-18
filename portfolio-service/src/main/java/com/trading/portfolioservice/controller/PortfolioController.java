package com.trading.portfolioservice.controller;


import com.trading.portfolioservice.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {


    private final PortfolioService portfolioService;


    @GetMapping("/me")
    public ResponseEntity<Map<String,Object>> getMyPortfolio(@RequestHeader("X-User_Id") String userId) {

        return ResponseEntity.ok(
                portfolioService.getPortfolio(userId));

    }


    /**
     *
     * for internaal work
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String,Object>> getPortfolio(@PathVariable String userId) {

        return ResponseEntity.ok(
                portfolioService.getPortfolio(userId));
    }






}
