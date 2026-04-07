package com.example.demo.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.repository.ExcelRepository;

@Service
public class ExcelService {

    @Autowired
    private ExcelRepository excelRepository;

    // Método para el Objetivo 1: Registrar el archivo en la BD
    public Excel registrarMetadata(MultipartFile archivo, long proyectoId, Integer usuarioId) {
        Excel registro = new Excel();
        registro.setIdProyecto(proyectoId);
        registro.setIdUsuario(usuarioId);
        registro.setFechaSubida(LocalDate.now());
        registro.setRutaArchivo("uploads/" + archivo.getOriginalFilename());
        return excelRepository.save(registro);
    }

    public int crearYGuardarExcel(List<DetalleEstimacion> datos) {
        if (datos == null || datos.isEmpty()) {
            return 0;
        }
        return datos.size();
    }
}