package com.example.demo.services;

import java.util.ArrayList;
import java.util.List;


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
    private ExcelService excelService;

    public int procesarExcel(MultipartFile archivo, long proyectoId, Integer usuarioId) throws Exception {
        
        // PASO 1: Usamos ExcelService para registrar la subida (Objetivo 1)
        Excel excelGuardado = excelService.registrarMetadata(archivo, proyectoId, usuarioId);

        // PASO 2: Leemos el Excel para las estimaciones (Objetivo 2)
        List<DetalleEstimacion> listaParaGuardar = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(archivo.getInputStream());

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet hoja = workbook.getSheetAt(i);
            int idDepartamento = determinarDepartamento(hoja.getSheetName());

            if (idDepartamento == -1) {
                continue; // Si el nombre de la hoja no es un departamento válido, se ignora
            }

            for (Row fila : hoja) {
                if (fila.getRowNum() == 0) continue; // Saltar cabecera

                if (fila.getCell(0) != null && fila.getCell(1) != null) {
                    DetalleEstimacion detalle = new DetalleEstimacion();
                    detalle.setIdProyecto(proyectoId);
                    detalle.setIdDepartamento(idDepartamento);
                    detalle.setIdExcel(excelGuardado.getIdExcel());
                    
                    detalle.setIdFase((int) fila.getCell(0).getNumericCellValue());
                    detalle.setTarea(fila.getCell(1).getStringCellValue());
                    
                    if (fila.getCell(2) != null) detalle.setTiempoMax(fila.getCell(2).getNumericCellValue());
                    if (fila.getCell(3) != null) detalle.setTiempoMin(fila.getCell(3).getNumericCellValue());

                    listaParaGuardar.add(detalle);
                }
            }
        }
        workbook.close();

        if (listaParaGuardar.isEmpty()) {
            throw new Exception("No se encontraron datos válidos en las hojas para registrar estimaciones.");
        }

        detalleEstimacionRepository.saveAll(listaParaGuardar);
        return listaParaGuardar.size();
    }

    private int determinarDepartamento(String nombre){
        return departamentoRepository.findByNombre(nombre).map(d -> d.getId()).orElse(-1);
    }

    public List<DetalleEstimacion> obtenerEstimacionesPorProyecto(Long idProyecto) {
        return detalleEstimacionRepository.findByIdProyecto(idProyecto);
    }
    }
