package com.example.demo.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.repository.ExcelRepository;

@Service
public class ExcelService {

    @Autowired
    private ExcelRepository excelRepository;

    // Este método es el que ahora recibe el objeto ya creado
    public Excel guardarDatosExcel(Excel excel) {
        return excelRepository.save(excel);
    }
   
    public int crearYGuardarExcel(List<DetalleEstimacion> datos) {
        if (datos == null || datos.isEmpty()) {
            return 0;
        }
        return datos.size();
    }
}