package com.example.demo.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.repository.ExcelRepository;

@Service
public class ExcelService {

    @Autowired
    private ExcelRepository excelRepository;

    @Transactional
    public Excel guardarDatosExcel(Excel excel) {
        if (excel.getIdProyecto() != null) {
            excelRepository.desactivarExcelsAnteriores(excel.getIdProyecto());
        }
        return excelRepository.save(excel);
    }
   
    public int crearYGuardarExcel(List<DetalleEstimacion> datos) {
        if (datos == null || datos.isEmpty()) {
            return 0;
        }
        return datos.size();
    }

    public Excel obtenerExcelVigentePorProyecto(Long idProyecto) {
        return excelRepository.findFirstByIdProyectoAndVigenteTrue(idProyecto);
    }
}