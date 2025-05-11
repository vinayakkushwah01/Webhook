package com.qualifier.service;

import org.springframework.web.client.RestTemplate;

import com.qualifier.model.WRequest;
import com.qualifier.model.WResponse;

import jakarta.annotation.PostConstruct;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class WService {
    @Value("${spring.application.name}")
    private String name;

    @Value("${app.regNo}")
    private String regNo;

    @Value("${spring.application.email}")
    private String email;

    private final RestTemplate restTemplate;

    public WService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Init method to set up the service
     @PostConstruct
    public void init() {
        WRequest request = new WRequest();
        request.setName(name);
        request.setRegNo(regNo);
        request.setEmail(email);

        ResponseEntity<WResponse> response = restTemplate.postForEntity(
                "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA",
                request,
                WResponse.class
        );
        System.out.println("Response from webhook generation request : " + response.getBody());

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            WResponse wResponse = response.getBody();

        
            // Sql Q1 from the pdf Link based on registration number as it is REG12347
            // 47 == odd no so Q1 
            String finalSQL = "SELECT p.amount AS SALARY, " +
                  "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                  "FLOOR(DATEDIFF(CURDATE(), e.DOB) / 365) AS AGE, " +
                  "d.DEPARTMENT_NAME AS DEPARTMENT_NAME " +
                  "FROM PAYMENTS p " +
                  "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                  "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                  "WHERE DAY(p.PAYMENT_TIME) != 1 " +
                  "ORDER BY p.AMOUNT DESC " +
                  "LIMIT 1;";





            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", wResponse.getAccessToken());

            Map<String, String> body = Map.of("finalQuery", finalSQL);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> result = restTemplate.postForEntity(
                    wResponse.getWebhook(), entity, String.class
            );

            System.out.println("Response from webhook: " + result.getBody());
        }
    }
}
