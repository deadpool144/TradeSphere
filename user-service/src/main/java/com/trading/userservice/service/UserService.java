package com.trading.userservice.service;


import com.trading.userservice.dto.AuthResponse;
import com.trading.userservice.dto.LoginRequest;
import com.trading.userservice.dto.RegisterRequest;
import com.trading.userservice.dto.UserResponse;
import com.trading.userservice.entity.User;
import com.trading.userservice.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Key;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    // add exception and map layer

    private  final UserRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    private static final String USER_REGISTER_TOPIC = "user.registered";

    // REGISTER
    public AuthResponse register(RegisterRequest request) {

        log.info("Registering User {}", request.getEmail());

        if(userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException(
                    "User with email already exists"+request.getEmail());

//        User user =User.builder()
//                .firstName()
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setWalletBalance(
//                request.getInitialDeposit()!=null
//                ?request.getInitalDeposit()
//                        : BigDecimal.valueOf(10000)
//        );
        user.setWalletBalance(
                request.getBalance()!=null
                ?request.getBalance()
                        : BigDecimal.valueOf(10000));

        User savedUser = userRepository.save(user);

        log.info("User {} has been Registered", savedUser.getId());

        Map<String,Object> userRegisteredEvent = new HashMap<>();
        userRegisteredEvent.put("userId", savedUser.getId());
        userRegisteredEvent.put("email", savedUser.getEmail());
        userRegisteredEvent.put("firstName", savedUser.getFirstName());
        userRegisteredEvent.put("lastName", savedUser.getLastName());
        userRegisteredEvent.put("walletBalance", user.getWalletBalance());

        kafkaTemplate.send(USER_REGISTER_TOPIC, savedUser.getId(), userRegisteredEvent);

        String token= generateAccessToken(savedUser.getId(),savedUser.getEmail());
        String refreshToken=generateRefreshToken(savedUser.getId());
        return buildAuthResponse(savedUser,token,refreshToken);
    }

    //add funds
    public UserResponse addFunds(String userId, BigDecimal amount) {

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"+userId));

        user.setWalletBalance(user.getWalletBalance().add(amount));

        User savedUser = userRepository.save(user);
        log.info("Funds {} added has been added to {} user",amount, savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    public UserResponse deductFunds(String userId, BigDecimal amount) {

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"+userId));

        if(user.getWalletBalance().compareTo(amount)<0)
            throw new RuntimeException("Insufficient funds to deduct");

        user.setWalletBalance(user.getWalletBalance().subtract(amount));

        User savedUser = userRepository.save(user);
        log.info("Funds {} added has been deducted to {} user",amount, savedUser.getId());

        return mapToUserResponse(savedUser);
    }


    public UserResponse creditFunds(String userId, BigDecimal amount) {

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"+userId));

        user.setWalletBalance(user.getWalletBalance().add(amount));

        User savedUser = userRepository.save(user);
        log.info("Funds {} credited has been added to {} user",amount, savedUser.getId());

        return mapToUserResponse(savedUser);
    }


    // helper
    private String generateAccessToken(String userId, String email) {
        return Jwts.builder()
                .claim("userId",userId)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateRefreshToken(String userId) {
        return Jwts.builder()
                .claim("userId",userId)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+refreshExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setWalletBalance(user.getWalletBalance());
        response.setTokenType("Bearer");

        return response;
    }

    // LOGIN
    public AuthResponse login(LoginRequest request) {

        log.info("Login User {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new RuntimeException("User not found"+request.getEmail()));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Wrong password");

        log.info("Login User {} has been logged", user.getEmail());

        String token= generateAccessToken(user.getId(),user.getEmail());
        String refreshToken=generateRefreshToken(user.getId());
        return buildAuthResponse(user,token,refreshToken);


    }

    public UserResponse getUserById(String id) {
        log.info("Get User by ID {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("User not found"+id));


        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user)
    {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setWalletBalance(user.getWalletBalance());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }




}
