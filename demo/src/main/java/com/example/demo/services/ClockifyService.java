package com.example.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class ClockifyService {

    @Value("${clockify.api.key}")
    private String apiKey;

    @Value("${clockify.workspace.id}")
    private String workspaceId;

    @Value("${clockify.api.url}")
    private String apiUrl;

    @Autowired
    private RestTemplate restTemplate;

    public List<Map<String, Object>> obtenerProyectosDeClockify() {
        String url = apiUrl + "/workspaces/" + workspaceId + "/projects";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, List.class
        );

        return response.getBody();
    }
}