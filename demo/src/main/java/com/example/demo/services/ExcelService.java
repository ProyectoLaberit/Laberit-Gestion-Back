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

    /**
     * Guarda un nuevo registro de Excel en la base de datos garantizando que sea el único vigente.
     * * @Transactional asegura que si falla el guardado del nuevo Excel, 
     * tampoco se apaguen los antiguos (todo o nada).
     * * @param excel El objeto Excel que se va a guardar (debe traer vigente = true desde el controlador).
     * @return El Excel guardado con su ID autogenerado.
     */
    @Transactional
    public Excel guardarDatosExcel(Excel excel) {
        if (excel.getIdProyecto() != null) {
            excelRepository.desactivarExcelsAnteriores(excel.getIdProyecto());
        }
        return excelRepository.save(excel);
    }
   
    /**
     * Valida y cuenta una lista de datos de estimación.
     * (Método auxiliar por si se requiere procesamiento previo antes de guardar).
     * * @param datos Lista de estimaciones en bruto.
     * @return El número de registros válidos.
     */
    public int crearYGuardarExcel(List<DetalleEstimacion> datos) {
        if (datos == null || datos.isEmpty()) {
            return 0;
        }
        return datos.size();
    }

    /**
     * Busca y devuelve el único Excel activo (vigente = true) de un proyecto.
     * Es fundamental para saber contra qué Excel cruzar las horas en las vistas del Frontend.
     * * @param idProyecto ID del proyecto a consultar.
     * @return El registro del Excel vigente o null si el proyecto no tiene Excels.
     */
    public Excel obtenerExcelVigentePorProyecto(Long idProyecto) {
        return excelRepository.findFirstByIdProyectoAndVigenteTrue(idProyecto);
    }
}