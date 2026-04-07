package com.example.demo.services;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.repository.DepartamentoRepository;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.repository.ExcelRepository;

@Service
public class DetalleEstimacionService {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;
    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private ExcelRepository excelRepository;

   public int procesarExcell(MultipartFile archivo, long proyectoId, Integer usuarioId) throws Exception {
        
        // OBJETIVO 1: Guardar metadatos en tabla 'excel'
        Excel registroExcel = new Excel();
        registroExcel.setIdProyecto(proyectoId);
        registroExcel.setIdUsuario(usuarioId);
        registroExcel.setFechaSubida(LocalDate.now());
        registroExcel.setRutaArchivo("uploads/" + archivo.getOriginalFilename());
        
        Excel excelGuardado = excelRepository.save(registroExcel);

        // OBJETIVO 2: Parsear y guardar en 'detalle_estimacion' vinculando IDs
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

                if (fila.getCell(0) != null && fila.getCell(1) != null) {
                    DetalleEstimacion detalle = new DetalleEstimacion();
                    detalle.setIdProyecto(proyectoId);
                    detalle.setIdDepartamento(idDepartamento);
                    detalle.setIdExcel(excelGuardado.getIdExcel());
                    
                    detalle.setIdFase((int) fila.getCell(0).getNumericCellValue());
                    detalle.setTarea(fila.getCell(1).getStringCellValue());
                    
                    if (fila.getCell(2) != null) {
                        detalle.setTiempoMax(fila.getCell(2).getNumericCellValue());
                    }
                    if (fila.getCell(3) != null) {
                        detalle.setTiempoMin(fila.getCell(3).getNumericCellValue());
                    }

                    listaParaGuardar.add(detalle);
                }
            }
        }
        
        workbook.close();

        if (listaParaGuardar.isEmpty()) {
            throw new Exception("Error: No se guardaron estimaciones. Revisa los nombres de las hojas del Excel.");
        }

        detalleEstimacionRepository.saveAll(listaParaGuardar);
        
        return listaParaGuardar.size();
    }

    private int determinarDepartamento(String nombre){

        return departamentoRepository.findByNombre(nombre).map(departamento -> departamento.getId()).orElse(-1);
       
    }

    

    public List<DetalleEstimacion> obtenerEstimacionesPorProyecto(Long idProyecto) {
        return detalleEstimacionRepository.findByIdProyecto(idProyecto);
    }

    }
