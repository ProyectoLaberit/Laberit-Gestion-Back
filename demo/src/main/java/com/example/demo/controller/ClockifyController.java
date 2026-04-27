package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ClockifyTareaDTO;
import com.example.demo.dto.ProyectoClockifyDTO;
import com.example.demo.services.ClockifyService;


@RestController
@RequestMapping("/api/clockify")
@CrossOrigin(origins = "*")

public class ClockifyController {

    @Autowired
    private ClockifyService clockifyService;

    @GetMapping("/{id}/{subfase}")
    public ApiResponse obtenerTareas(@PathVariable Long id, @PathVariable String subfase) {

        List<ClockifyTareaDTO> todasLasTareas = clockifyService.obtenerTareasPorSubfase(id, subfase);

        System.out.println(todasLasTareas);


        return new ApiResponse("a", false, todasLasTareas);
    }
    

    @GetMapping("/externos")
    public ApiResponse obtenerProyectosClockifyNoBD() {

        List<ProyectoClockifyDTO> proyectosClockify = clockifyService.obtenerProyectosNuevosDTO();

        return new ApiResponse(null, true, proyectosClockify);
        
    }

    @GetMapping("/fallos/{id}")
    public ApiResponse getFallidos(@PathVariable Long id) {

        List<ClockifyTareaDTO> tareasFallidas = clockifyService.obtenerEntradasInvalidas(id);
        return new ApiResponse("tareasFaliidas", true, tareasFallidas);
    }
    
}
