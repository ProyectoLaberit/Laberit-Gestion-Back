package com.example.demo.controller;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.annotation.Auditable;
import com.example.demo.entity.Excel;
import com.example.demo.services.ExcelService;
import com.example.demo.services.excel.GeneradorInformeExcelService;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @Autowired
    @Qualifier("generadorInformeExcelService")
    private GeneradorInformeExcelService generadorInformeExcelService;

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

    /**
     * Genera el Reporte Analítico inteligente cruzando datos de estimaciones, GitLab y Clockify.
     * @param idProyecto ID del proyecto a analizar.
     * @param idExcelElegido ID del Excel del historial (opcional).
     * @return byte[] El contenido del archivo Excel generado.
     */
    @Auditable(
        accion = "DESCARGAR_REPORTE_ANALITICO", 
        tabla = "proyecto",
        entidad = Excel.class, // Ajusta si la entidad auditada principal debe ser Proyecto.class
        descripcion = "Se descargó el Reporte Analítico del Proyecto ID: #idProyecto"
    )
    @GetMapping("/exportar-analitico/{idProyecto}/{idExcel}")
    public ResponseEntity<byte[]> exportarExcelAnalitico(@PathVariable Long idProyecto, @PathVariable Integer idExcel) {
        try {
            // Llamamos a nuestro nuevo servicio
            ByteArrayInputStream bis = generadorInformeExcelService.generarExcelAnalitico(idProyecto, idExcel);
            
            // Convertimos el InputStream a un array de bytes
            byte[] archivoExcel = bis.readAllBytes();
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Reporte_Analitico_Proyecto_" + idProyecto + ".xlsx\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            
            return new ResponseEntity<>(archivoExcel, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            
        }
    }
}
