package com.example.demo.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.DetalleEstimacion;

@Service
public class ExcelService {
   
  public int crearYGuardarExcel(List<DetalleEstimacion> datos) {
        
        if (datos == null || datos.isEmpty()) {
            return 0;
        }
        
        int filasGuardadas = datos.size();
        return filasGuardadas;
    }
}
