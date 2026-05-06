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

    public Double obtenerSumaHorasValidas(Long idDetalleEstimacion) {
        Double suma = repository.sumarHorasValidas(idDetalleEstimacion);
        if (suma != null) {
            return suma;
        } else {
            return 0.0;
        }
    }
    
    public List<ImputacionClockify> obtenerImputacionesValidas(Long idDetalleEstimacion) {
        return repository.findByIdDetalleEstimacionAndValidaTrue(idDetalleEstimacion);
    }

    public List<ImputacionClockify> obtenerHuerfanas(Long idProyecto) {
        return repository.findByIdProyectoAndValidaFalse(idProyecto);
    }

    public ImputacionClockify vincularImputacionManual(Long idImputacionClockify, Long idDetalleEstimacion) {
        ImputacionClockify imputacion = repository.findById(idImputacionClockify).orElse(null);
        
        if (imputacion != null && !imputacion.getValida()) {
            imputacion.setValida(true);
            imputacion.setIdDetalleEstimacion(idDetalleEstimacion);
            return repository.save(imputacion);
        }
        
        return null; 
    }

    public boolean existeImputacion(String idClockifyOriginal) {
        return repository.existsByIdClockifyOriginal(idClockifyOriginal);
    }
    
    public List<ImputacionClockify> guardarImputacionesMasivas(List<ImputacionClockify> nuevasImputaciones) {
        return repository.saveAll(nuevasImputaciones);
    }

    public ImputacionClockify obtenerPorIdClockify(String idClockifyOriginal) {
        return repository.findByIdClockifyOriginal(idClockifyOriginal);
    }

    public ImputacionClockify actualizarImputacion(ImputacionClockify imputacion) {
        return repository.save(imputacion);
    }
}