package com.example.demo.services;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.repository.DepartamentoRepository;
import com.example.demo.repository.DetalleEstimacionRepository;

@Service
public class DetalleEstimacionService {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;
    @Autowired
    private DepartamentoRepository departamentoRepository;

    public void procesarExcell(MultipartFile archivo, long proyectoId) throws Exception {
        // Aquí iría la lógica para procesar el archivo Excel y guardar los detalles de estimación
        // en la base de datos utilizando el detalleEstimacionRepository.
        List<DetalleEstimacion> listaParaGuardar = new ArrayList<>();
        // Lógica para leer el archivo Excel y convertirlo en objetos DetalleEstimacion
        try(InputStream flujoDatos = archivo.getInputStream();
            Workbook miExcel = new XSSFWorkbook(flujoDatos)) {
         
                for(int i = 0; i < miExcel.getNumberOfSheets();i++){
                    Sheet pestanaActual = miExcel.getSheetAt(i);
                    String nombrePestana = pestanaActual.getSheetName().toUpperCase();

                    int departamentoId = determinarDepartamento(nombrePestana);
                    if(departamentoId == -1){    
                        continue; // Si no se reconoce el nombre de la pestaña, se salta a la siguiente
                    }
                 for(Row filaActual : pestanaActual){
                    if(filaActual == null){
                        continue; // Si la fila es nula, se salta a la siguiente
                    }
                    Cell celdaTarea = filaActual.getCell(0); 
                    Cell celdaMin = filaActual.getCell(3);
                    Cell celdaMax = filaActual.getCell(4);      

                    if(celdaTarea == null || celdaTarea.getCellType() != CellType.STRING){
                        continue;
                    }
                    if(celdaMin == null || celdaMin.getCellType() != CellType.NUMERIC){
                        continue;
                    }
                    if(celdaMax == null || celdaMax.getCellType() != CellType.NUMERIC){
                        continue;
                    }

                    DetalleEstimacion nuevaTarea = new DetalleEstimacion();
                    nuevaTarea.setIdProyecto(proyectoId);
                    nuevaTarea.setIdDepartamento(departamentoId);
                    nuevaTarea.setIdFase(1); // Aquí podrías agregar lógica para determinar la fase si es necesario

                    nuevaTarea.setTarea(celdaTarea.getStringCellValue());
                    nuevaTarea.setTiempoMin(celdaMin.getNumericCellValue());
                    nuevaTarea.setTiempoMax(celdaMax.getNumericCellValue());

                    listaParaGuardar.add(nuevaTarea);
                 }   
                }
        
    }
        detalleEstimacionRepository.saveAll(listaParaGuardar);
}

    private int determinarDepartamento(String nombre){

        return departamentoRepository.findByNombre(nombre).map(departamento -> departamento.getId()).orElse(-1);
       
    }

    }
