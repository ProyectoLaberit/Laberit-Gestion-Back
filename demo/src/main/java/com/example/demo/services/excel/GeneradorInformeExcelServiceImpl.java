package com.example.demo.services.excel;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map; // Faltaba este import

import org.apache.poi.ss.usermodel.Sheet; // CORREGIDO: 'ss' en lugar de 'sl'
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // Faltaba el @Service

import com.example.demo.entity.ImputacionClockify;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.repository.ImputacionClockifyRepository;
import com.example.demo.repository.TareaProyectoRepository;
import com.example.demo.dto.excel.FilaComparativaDTO;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFColor;
import java.io.ByteArrayOutputStream;

@Service // Obligatorio para que Spring sepa que esto es un servicio
public class GeneradorInformeExcelServiceImpl implements GeneradorInformeExcelService {

    // Aquí sí podemos usar @Autowired (o private final con Lombok)
    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    @Autowired
    private ImputacionClockifyRepository imputacionClockifyRepository;

    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

   @Override
    public ByteArrayInputStream generarExcelAnalitico(Long idProyecto) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Map<String, CellStyle> estilos = crearEstilosCorporativos(workbook);

            generarHojaDashboard(workbook, estilos, idProyecto);
            generarHojaTareas(workbook, estilos, idProyecto);
            // generarHojaValidaciones(workbook, estilos, idProyecto); // Pendiente
            // generarHojaAvanceSemanal(workbook, estilos, idProyecto); // Pendiente

            workbook.write(out);
            workbook.dispose();
            
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte analítico Excel", e);
        }
    }

    // Aquí sí podemos tener métodos privados con lógica
    private void generarHojaDashboard(SXSSFWorkbook workbook, Map<String, CellStyle> estilos, Long idProyecto) {
        Sheet sheet = workbook.createSheet("1. DASHBOARD");
        
        // 1. OBTENCIÓN DE DATOS
        Double horasMaximas = detalleEstimacionRepository.obtenerTotalHorasMaximasProyecto(idProyecto);
        if (horasMaximas == null) {
            horasMaximas = 0.0;
        }
        
        // Obtenemos las imputaciones para calcular las horas reales totales
        List<ImputacionClockify> imputaciones = imputacionClockifyRepository.obtenerImputacionesValidasOrdenadas(idProyecto);
        double horasRealesTotales = 0.0;
        for (ImputacionClockify imp : imputaciones) {
            if (imp.getHorasTrabajadas() != null) {
                horasRealesTotales += imp.getHorasTrabajadas();
            }
        }
        
        // 2. CREACIÓN DE LA CABECERA CORPORATIVA
        Row filaCabecera = sheet.createRow(0);
        filaCabecera.setHeightInPoints(30);
        
        Cell celdaTitulo = filaCabecera.createCell(0);
        celdaTitulo.setCellValue("PROYECTO: ID " + idProyecto + " - ESTADO: En curso");
        celdaTitulo.setCellStyle(estilos.get("cabecera"));
        
        // Fusionamos celdas para que el título ocupe todo el ancho superior (A1:G1)
        org.apache.poi.ss.util.CellRangeAddress regionCabecera = new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6);
        sheet.addMergedRegion(regionCabecera);
        
        // 3. SECCIÓN DE KPIs SUPERIORES
        Row filaKpisHeaders = sheet.createRow(2);
        Row filaKpisValores = sheet.createRow(3);
        
        // Tarjeta 1: Horas Estimadas Máximas
        filaKpisHeaders.createCell(1).setCellValue("HORAS EST. MÁXIMAS");
        filaKpisValores.createCell(1).setCellValue(horasMaximas + "h");
        
        // Tarjeta 2: Horas Reales
        filaKpisHeaders.createCell(3).setCellValue("HORAS REALES");
        filaKpisValores.createCell(3).setCellValue(horasRealesTotales + "h");
        
        // Tarjeta 3: Desviación vs Máximo
        double desviacionTotal = horasRealesTotales - horasMaximas;
        filaKpisHeaders.createCell(5).setCellValue("DESVIACIÓN VS MÁX");
        filaKpisValores.createCell(5).setCellValue(desviacionTotal > 0 ? "+" + desviacionTotal + "h" : desviacionTotal + "h");
        
        // Ajuste de anchos de columna para mejor visualización
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(3, 6000);
        sheet.setColumnWidth(5, 6000);
    }


    private Map<String, CellStyle> crearEstilosCorporativos(SXSSFWorkbook workbook) {
        Map<String, CellStyle> estilos = new java.util.HashMap<>();

        CellStyle estiloCabecera = workbook.createCellStyle();
        estiloCabecera.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estiloCabecera.setAlignment(HorizontalAlignment.CENTER);
        estiloCabecera.setVerticalAlignment(VerticalAlignment.CENTER);

        Font fuenteCabecera = workbook.createFont();
        fuenteCabecera.setBold(true);
        fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());
        estiloCabecera.setFont(fuenteCabecera);

        estilos.put("cabecera", estiloCabecera);
        return estilos;
    }

    private void generarHojaTareas(SXSSFWorkbook workbook, Map<String, CellStyle> estilos, Long idProyecto) {
        Sheet sheet = workbook.createSheet("2. TAREAS");
        
        // 1. CREACIÓN DE CABECERA
        String[] columnas = {
            "ID GITLAB", "FASE", "TAREA", "DEPARTAMENTO", 
            "EST. MÍN (h)", "EST. MÁX (h)", "HORAS REALES (h)", 
            "DESVIACIÓN (h)", "% DESVIACIÓN", "ESTADO GITLAB"
        };
        
        Row filaCabecera = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            Cell celda = filaCabecera.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilos.get("cabecera"));
            sheet.setColumnWidth(i, 4500);
        }
        
        // 2. OBTENCIÓN Y POBLADO DE DATOS
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto);
        
        int rowNum = 1;
        for (FilaComparativaDTO tarea : tareas) {
            Row fila = sheet.createRow(rowNum++);
            
            fila.createCell(0).setCellValue(tarea.getIdGitlab() != null ? tarea.getIdGitlab() : "-");
            fila.createCell(1).setCellValue(tarea.getFase());
            fila.createCell(2).setCellValue(tarea.getTarea());
            fila.createCell(3).setCellValue(tarea.getDepartamento());
            fila.createCell(4).setCellValue(tarea.getEstimacionMinima());
            fila.createCell(5).setCellValue(tarea.getEstimacionMaxima());
            fila.createCell(6).setCellValue(tarea.getHorasReales());
            fila.createCell(7).setCellValue(tarea.getDesviacionHoras());
            fila.createCell(8).setCellValue(tarea.getDesviacionPorcentaje());
            fila.createCell(9).setCellValue(tarea.getEstadoGitlab());
        }
    }
}