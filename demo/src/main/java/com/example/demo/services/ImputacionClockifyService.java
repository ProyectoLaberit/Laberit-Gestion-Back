package com.example.demo.services;

import com.example.demo.entity.ImputacionClockify;
import com.example.demo.repository.ImputacionClockifyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImputacionClockifyService {

    @Autowired
    private ImputacionClockifyRepository repository;

    // Este es el método que calcula la suma
    public Double obtenerSumaHorasValidas(Long idDetalleEstimacion) {
        Double suma = repository.sumarHorasValidas(idDetalleEstimacion);
        return suma != null ? suma : 0.0;
    }
    
    // Traer lista para la tabla (Automáticas y Manuales)
    public List<ImputacionClockify> obtenerImputacionesValidas(Long idDetalleEstimacion) {
        // Usamos List.of para pasarle los dos estados válidos
        return repository.findByIdDetalleEstimacionAndEstadoIn(
                idDetalleEstimacion, java.util.List.of("AUTOMATICA", "MANUAL")
        );
    }

    // Traer lista para el desplegable (Solo Huérfanas)
    public List<ImputacionClockify> obtenerHuerfanas(Long idDetalleEstimacion) {
        return repository.findByIdDetalleEstimacionAndEstado(idDetalleEstimacion, "HUERFANA");
    }

    // Cambiar una imputación de Huérfana a Manual
    public ImputacionClockify vincularImputacionManual(Long idImputacionClockify) {
        ImputacionClockify imputacion = repository.findById(idImputacionClockify).orElse(null);
        
        if (imputacion != null && "HUERFANA".equals(imputacion.getEstado())) {
            imputacion.setEstado("MANUAL");
            return repository.save(imputacion);
        }
        return null; // Devuelve null si no existe o si ya estaba validada
    }
}