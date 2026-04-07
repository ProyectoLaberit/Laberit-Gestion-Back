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
      // PASO 1: Registrar el archivo en la tabla 'excel'
        Excel registroExcel = new Excel();
        registroExcel.setIdProyecto(proyectoId);
        registroExcel.setIdUsuario(usuarioId);
        registroExcel.setFechaSubida(LocalDate.now());
        registroExcel.setRutaArchivo("uploads/" + archivo.getOriginalFilename());
        
        // Corrección 2: Usar la instancia 'excelRepository' (minúscula), no la clase estática
        Excel excelGuardado = excelRepository.save(registroExcel);
        Integer idExcelGenerado = excelGuardado.getIdExcel();

        // PASO 2: Leer el contenido del Excel
        List<DetalleEstimacion> listaParaGuardar = new ArrayList<>();
        // Corrección 3: Usar 'archivo' en lugar de 'archivoSubido'
        Workbook workbook = WorkbookFactory.create(archivo.getInputStream());

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet hoja = workbook.getSheetAt(i);
            int idDepartamento = determinarDepartamento(hoja.getSheetName());

            // Corrección 4: Validar contra -1, ya que el método retorna int primitivo, no null
            if (idDepartamento == -1) {
                continue;
            }

            for (Row fila : hoja) {
                if (fila.getRowNum() == 0) {
                    continue;
                }

                DetalleEstimacion detalle = new DetalleEstimacion();
                detalle.setIdProyecto(proyectoId);
                detalle.setIdDepartamento(idDepartamento);
                detalle.setIdExcel(idExcelGenerado);
                
                // Asignación de datos del Excel
                detalle.setIdFase((int) fila.getCell(0).getNumericCellValue());
                detalle.setTarea(fila.getCell(1).getStringCellValue());
                detalle.setTiempoMax(fila.getCell(2).getNumericCellValue());
                detalle.setTiempoMin(fila.getCell(3).getNumericCellValue());

                listaParaGuardar.add(detalle);
            }
        }

        // PASO 3: Guardado masivo de las tareas vinculadas
        if (!listaParaGuardar.isEmpty()) {
            detalleEstimacionRepository.saveAll(listaParaGuardar);
        }
        
        workbook.close();
        return listaParaGuardar.size();
}

    private int determinarDepartamento(String nombre){

        return departamentoRepository.findByNombre(nombre).map(departamento -> departamento.getId()).orElse(-1);
       
    }

    }
