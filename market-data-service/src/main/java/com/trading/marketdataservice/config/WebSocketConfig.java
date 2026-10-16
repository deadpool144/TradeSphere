package com.trading.marketdataservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 *  working
 *   client coonect to ws endpoint
 *   sub to topic/prices/{symbol}
 *   server pub to topic
 *   client update in real time- no pooling needed
 *
 *   similar to zerodha .... stream real time prices
 */

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //topic prefixed with /topic - broadcast to all subs
        registry.enableSimpleBroker("/topic");
        //application desitination prefix
        //client messages to /app are routed to @messagingMapping methods
        registry.setApplicationDestinationPrefixes("/app");

    }

    public void registerStompEndpoints(StompEndpointRegistry registry){

        //websocket endpoints - client connect here
        //sockJs fallbaack for browser that dont support websocket
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

}
