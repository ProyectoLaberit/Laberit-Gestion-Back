package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.annotation.Auditable;
import com.example.demo.entity.Excel;
import com.example.demo.services.ExcelService;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    /**
     * Genera un archivo Excel completo basado en la plantilla, inyectando los datos de la BD.
     * @param idExcel ID del registro de estimaciones a exportar.
     * @return byte[] El contenido del archivo Excel generado.
     */
    @Auditable(
        accion = "DESCARGAR_EXCEL", 
        tabla = "excel", 
        entidad = Excel.class,
        descripcion = "Se descargó el Excel con ID: #idExcel (Proyecto ID: #{#antiguo != null ? #antiguo.idProyecto : 'Desconocido'})"
    )
    @GetMapping("/exportar/{idExcel}")
    public ResponseEntity<byte[]> exportarExcel(@PathVariable Integer idExcel) {
        try {
            byte[] archivoExcel = excelService.exportarExcelCompleto(idExcel);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Estimacion_Proyecto_" + idExcel + ".xlsx\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            
            return new ResponseEntity<>(archivoExcel, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}