package com.example.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.entity.ApiConfig;
import com.example.demo.repository.ApiConfigRepository;

import java.util.List;
import java.util.Map;

@Service
public class ClockifyService {

    @Autowired
    ApiConfigRepository repositorioApi;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${clockify.workspace.id}")
    private String workspaceId;

    public List<Map<String, Object>> obtenerProyectosDeClockify() {
        ApiConfig clockify = repositorioApi.findByNombre("Clockify Maestro");
        
        String url = clockify.getUrlReal() + "/workspaces/" + workspaceId + "/projects";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", clockify.getClave());
        
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, List.class
        );

        return response.getBody();
    }
}