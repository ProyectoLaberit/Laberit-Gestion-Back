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
import com.example.demo.repository.FaseRepository;
@Service
public class DetalleEstimacionService {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;
    
    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private FaseRepository faseRepository;

    @Autowired
    private ExcelService excelService;

    public int procesarExcel(MultipartFile archivo, long proyectoId, Integer usuarioId) throws Exception {
        
        Excel registroExcel = new Excel();
        registroExcel.setIdProyecto(proyectoId);
        registroExcel.setIdUsuario(usuarioId);
        registroExcel.setFechaSubida(LocalDate.now());
        registroExcel.setRutaArchivo("uploads/" + archivo.getOriginalFilename());
        
        Excel excelGuardado = excelService.guardarDatosExcel(registroExcel);
        Integer idExcelGenerado = excelGuardado.getIdExcel();

        // Evitar errores de unboxing y FK
        Integer fasePorDefecto = faseRepository.findAll().stream()
                .findFirst()
                .map(f -> f.getId())
                .orElseThrow(() -> new Exception("Debe haber registros en la tabla 'fase'."));

        List<DetalleEstimacion> listaParaGuardar = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(archivo.getInputStream());

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet hoja = workbook.getSheetAt(i);
            int idDepartamento = determinarDepartamento(hoja.getSheetName());

            if (idDepartamento == -1) {
                continue;
            }

            Integer idFaseActual = fasePorDefecto;

            for (Row fila : hoja) {
                if (fila.getRowNum() < 1) { 
                    continue;
                }

                String tareaEncontrada = null;
                Double tiempo1 = null;
                Double tiempo2 = null;

                Cell cell0 = fila.getCell(0);
                if (cell0 != null && cell0.getCellType() == CellType.STRING) {
                    String contenido0 = cell0.getStringCellValue().trim();
                    if (!contenido0.isEmpty()) {
                        Integer idFaseEncontrada = determinarFase(contenido0);
                        if (idFaseEncontrada != null) {
                            idFaseActual = idFaseEncontrada;
                        }
                    }
                }

                for (Cell celda : fila) {
                    if (celda.getCellType() == CellType.STRING) {
                        String texto = celda.getStringCellValue().trim();
                        if (!texto.isEmpty() && determinarFase(texto) == null && tareaEncontrada == null) {
                            tareaEncontrada = texto;
                        }
                    } else if (celda.getCellType() == CellType.NUMERIC) {
                        if (tiempo1 == null) {
                            tiempo1 = celda.getNumericCellValue();
                        } else if (tiempo2 == null) {
                            tiempo2 = celda.getNumericCellValue();
                        }
                    }
                }

                if (tareaEncontrada != null && (tiempo1 != null || tiempo2 != null)) {
                    DetalleEstimacion detalle = new DetalleEstimacion();
                    detalle.setIdProyecto(proyectoId);
                    detalle.setIdDepartamento(idDepartamento);
                    detalle.setIdExcel(idExcelGenerado);
                    detalle.setIdFase(idFaseActual != null ? idFaseActual : fasePorDefecto);
                    detalle.setTarea(tareaEncontrada);
                    
                    // Manejo seguro de nulos para evitar NullPointerException en Math.min/max
                    if (tiempo1 != null && tiempo2 != null) {
                        detalle.setTiempoMin(Math.min(tiempo1, tiempo2));
                        detalle.setTiempoMax(Math.max(tiempo1, tiempo2));
                    } else {
                        double valorFinal = (tiempo1 != null) ? tiempo1 : tiempo2;
                        detalle.setTiempoMin(valorFinal);
                        detalle.setTiempoMax(valorFinal);
                    }

                    listaParaGuardar.add(detalle);
                }
            }
        }
        
        workbook.close();

        if (listaParaGuardar.isEmpty()) {
            throw new Exception("Error: No se extrajeron datos válidos.");
        }

        detalleEstimacionRepository.saveAll(listaParaGuardar);
        
        return listaParaGuardar.size();
    }

    private int determinarDepartamento(String nombre) {
        return departamentoRepository.findAll().stream()
                .filter(d -> d.getNombre().equalsIgnoreCase(nombre.trim()))
                .map(d -> d.getId())
                .findFirst()
                .orElse(-1);
    }

    private Integer determinarFase(String nombre) {
        return faseRepository.findAll().stream()
                .filter(f -> f.getNombre().equalsIgnoreCase(nombre.trim()))
                .map(f -> f.getId())
                .findFirst()
                .orElse(null);
    }

    public List<DetalleEstimacion> obtenerEstimacionesPorProyecto(Long idProyecto) {
        return detalleEstimacionRepository.findByIdProyecto(idProyecto);
    }
}