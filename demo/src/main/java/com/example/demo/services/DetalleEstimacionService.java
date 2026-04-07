package com.example.demo.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.repository.DepartamentoRepository;
import com.example.demo.repository.DetalleEstimacionRepository;

@Service
public class DetalleEstimacionService {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;
    
    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private ExcelService excelService; // Usamos el service, no el repository directamente

    public int procesarExcel(MultipartFile archivo, long proyectoId, Integer usuarioId) throws Exception {
        
        // OBJETIVO 1: Preparar y guardar metadatos usando el ExcelService
        Excel registroExcel = new Excel();
        registroExcel.setIdProyecto(proyectoId);
        registroExcel.setIdUsuario(usuarioId);
        registroExcel.setFechaSubida(LocalDate.now());
        registroExcel.setRutaArchivo("uploads/" + archivo.getOriginalFilename());
        
        // Llamada limpia al service
        Excel excelGuardado = excelService.guardarDatosExcel(registroExcel);
        Integer idExcelGenerado = excelGuardado.getIdExcel();

        // OBJETIVO 2: Procesar las filas del Excel
        List<DetalleEstimacion> listaParaGuardar = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(archivo.getInputStream());

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet hoja = workbook.getSheetAt(i);
            int idDepartamento = determinarDepartamento(hoja.getSheetName());

            if (idDepartamento == -1) {
                continue;
            }

            for (Row fila : hoja) {
                if (fila.getRowNum() == 0) {
                    continue;
                }

                Cell celdaTarea = fila.getCell(0);
                if (celdaTarea == null || celdaTarea.toString().trim().isEmpty()) {
                    continue;
                }

                try {
                    DetalleEstimacion detalle = new DetalleEstimacion();
                    detalle.setIdProyecto(proyectoId);
                    detalle.setIdDepartamento(idDepartamento);
                    detalle.setIdExcel(idExcelGenerado);
                    detalle.setIdFase(0); 

                    detalle.setTarea(celdaTarea.toString());

                    if (fila.getCell(1) != null && fila.getCell(1).getCellType() == CellType.NUMERIC) {
                        detalle.setTiempoMin(fila.getCell(1).getNumericCellValue());
                    }

                    if (fila.getCell(2) != null && fila.getCell(2).getCellType() == CellType.NUMERIC) {
                        detalle.setTiempoMax(fila.getCell(2).getNumericCellValue());
                    }

                    listaParaGuardar.add(detalle);
                } catch (Exception e) {
                    continue;
                }
            }
        }
        workbook.close();

        if (listaParaGuardar.isEmpty()) {
            throw new Exception("Error: No se extrajeron datos válidos del Excel.");
        }

        detalleEstimacionRepository.saveAll(listaParaGuardar);
        return listaParaGuardar.size();
    }

    private int determinarDepartamento(String nombre) {
        return departamentoRepository.findByNombre(nombre).map(d -> d.getId()).orElse(-1);
    }

    public List<DetalleEstimacion> obtenerEstimacionesPorProyecto(Long idProyecto) {
        return detalleEstimacionRepository.findByIdProyecto(idProyecto);
    }
}
