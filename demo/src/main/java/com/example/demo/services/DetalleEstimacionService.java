package com.example.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.repository.DetalleEstimacionRepository;

@Service
public class DetalleEstimacionService {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    public void procesarExcell(MultipartFile archivo, long proyectoId) {
        // Aquí iría la lógica para procesar el archivo Excel y guardar los detalles de estimación
        // en la base de datos utilizando el detalleEstimacionRepository.
        
    }
}
