package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Departamento;
import com.example.demo.repository.DepartamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departamentos")
@CrossOrigin(origins = "*")
public class DepartamentoController {

    @Autowired
    private DepartamentoRepository departamentoRepository;

    /**
     * Metodo que devuelve los departamentos existentes en la base de datos
     * @return ApiResponse json con los departamentos existentes en la base de datos
     */
    @GetMapping
    public ApiResponse listarDepartamentos() {
        try {
            List<Departamento> departamentos = departamentoRepository.findAll();
            return new ApiResponse("Departamentos recuperados", true, departamentos);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar departamentos: " + e.getMessage(), false, null);
        }
    }
}
