package com.example.demo.services.excel;

import com.example.demo.repository.GitLabTareaRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map; // Faltaba este import

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // Faltaba el @Service

import com.example.demo.entity.GitLabTarea;
import com.example.demo.entity.ImputacionClockify;
import com.example.demo.entity.TareaProyecto;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.repository.ImputacionClockifyRepository;
import com.example.demo.repository.TareaProyectoRepository;
import com.example.demo.dto.excel.FilaComparativaDTO;
import com.example.demo.dto.excel.ProblemasDetectadosDTO;

import org.apache.poi.xssf.usermodel.XSSFColor;
import java.io.ByteArrayOutputStream;
   import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;

// CORREGIDO: 'ss' en lugar de 'sl'

// Obligatorio para que Spring sepa que esto es un servicio
@Service("generadorInformeExcelService")
public class GeneradorInformeExcelServiceImpl implements GeneradorInformeExcelService {

    private final GitLabTareaRepository gitLabTareaRepository;

    // Aquí sí podemos usar @Autowired (o private final con Lombok)
    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    @Autowired
    private ImputacionClockifyRepository imputacionClockifyRepository;

    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

    GeneradorInformeExcelServiceImpl(GitLabTareaRepository gitLabTareaRepository) {
        this.gitLabTareaRepository = gitLabTareaRepository;
    }

 @Override
public ByteArrayInputStream generarExcelAnalitico(Long idProyecto) {
    try (InputStream is = getClass().getResourceAsStream("/plantillas/dashboard_template.xlsx");
         Workbook workbook = WorkbookFactory.create(is); 
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

        Map<String, CellStyle> estilos = crearEstilosCorporativos(workbook);

        // Llamamos al método con el nombre correcto
        generarDashboardConPlantilla(workbook, estilos, idProyecto);
        generarHojaTareas(workbook, estilos, idProyecto);

        workbook.write(out);
        return new ByteArrayInputStream(out.toByteArray());

    } catch (Exception e) {
        throw new RuntimeException("Error al generar el reporte analítico Excel", e);
    }
}

    // Aquí sí podemos tener métodos privados con lógica
private void generarDashboardConPlantilla(Workbook workbook, Map<String, CellStyle> estilos, Long idProyecto) {
    Sheet sheet = workbook.getSheetAt(0);

    // 1. KPIs Principales (Redondeados)
    double horasMaximas = detalleEstimacionRepository.obtenerTotalHorasMaximasProyecto(idProyecto);
    double horasReales = imputacionClockifyRepository.sumarHorasTotalesProyecto(idProyecto);
    double desviacion = horasReales - horasMaximas;

    escribirCeldaPorNombre(workbook, sheet, "KPI_MAXIMAS", Math.round(horasMaximas));
    escribirCeldaPorNombre(workbook, sheet, "KPI_REALES", Math.round(horasReales));
    escribirCeldaPorNombre(workbook, sheet, "KPI_DESVIACION", Math.round(desviacion));

    // 2. Obtener TODAS las tareas para extraer Top 10 y Gráficos
    List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto);

    // 3. Procesar y rellenar TOP 10 Desviaciones
    List<Object[]> top10 = tareas.stream()
            .sorted((t1, t2) -> Double.compare(t2.getDesviacionHoras(), t1.getDesviacionHoras()))
            .limit(10)
            .map(t -> new Object[]{
                    t.getIdGitlab() != null ? t.getIdGitlab() : "-",
                    t.getFase(),
                    t.getTarea(),
                    Math.round(t.getEstimacionMaxima()),
                    Math.round(t.getHorasReales()),
                    Math.round(t.getDesviacionHoras())
            })
            .collect(Collectors.toList());
    
    escribirListaDesdeAncla(workbook, sheet, "TOP10_INICIO", top10);

    // 4. Procesar y rellenar Gráfico de Departamentos (Agrupación por suma de horas reales)
    List<Object[]> datosDepartamentos = tareas.stream()
            .collect(Collectors.groupingBy(FilaComparativaDTO::getDepartamento, Collectors.summingDouble(FilaComparativaDTO::getHorasReales)))
            .entrySet().stream()
            .map(e -> new Object[]{e.getKey(), Math.round(e.getValue())})
            .collect(Collectors.toList());
            
    escribirListaDesdeAncla(workbook, sheet, "GRAF_DEP_INICIO", datosDepartamentos);

    // 5. Procesar y rellenar Gráfico de Fases (Agrupación por suma de horas reales)
    List<Object[]> datosFases = tareas.stream()
            .collect(Collectors.groupingBy(FilaComparativaDTO::getFase, Collectors.summingDouble(FilaComparativaDTO::getHorasReales)))
            .entrySet().stream()
            .map(e -> new Object[]{e.getKey(), Math.round(e.getValue())})
            .collect(Collectors.toList());
            
    escribirListaDesdeAncla(workbook, sheet, "GRAF_FASE_INICIO", datosFases);
}
private void escribirCelda(Sheet sheet, int row, int col, double valor, CellStyle estilo) {
    Row fila = sheet.getRow(row);
    if (fila == null) fila = sheet.createRow(row);
    
    Cell celda = fila.getCell(col);
    if (celda == null) celda = fila.createCell(col);
    
    celda.setCellValue(valor);
    if (estilo != null) {
        celda.setCellStyle(estilo);
    }
}



    private Map<String, CellStyle> crearEstilosCorporativos(Workbook workbook) {
        Map<String, CellStyle> estilos = new java.util.HashMap<>();
        DataFormat format = workbook.createDataFormat();

        // 1. Cabecera Roja
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

        // 2. Decimal Estandar
        CellStyle estiloDecimal = workbook.createCellStyle();
        estiloDecimal.setDataFormat(format.getFormat("#,##0.00"));
        estilos.put("decimal", estiloDecimal);

        // 3. Cabecera de Tarjetas KPI (Dashboard)
        CellStyle estiloKpiHeader = workbook.createCellStyle();
        estiloKpiHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloKpiHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estiloKpiHeader.setAlignment(HorizontalAlignment.CENTER);
        estiloKpiHeader.setBorderBottom(BorderStyle.THIN);
        estiloKpiHeader.setBorderTop(BorderStyle.THIN);
        estiloKpiHeader.setBorderLeft(BorderStyle.THIN);
        estiloKpiHeader.setBorderRight(BorderStyle.THIN);
        Font fuenteKpi = workbook.createFont();
        fuenteKpi.setBold(true);
        estiloKpiHeader.setFont(fuenteKpi);
        estilos.put("kpiHeader", estiloKpiHeader);

        // 4. Valores de Tarjetas KPI (Dashboard)
        CellStyle estiloKpiValor = workbook.createCellStyle();
        estiloKpiValor.setAlignment(HorizontalAlignment.CENTER);
        estiloKpiValor.setBorderBottom(BorderStyle.THIN);
        estiloKpiValor.setBorderTop(BorderStyle.THIN);
        estiloKpiValor.setBorderLeft(BorderStyle.THIN);
        estiloKpiValor.setBorderRight(BorderStyle.THIN);
        estiloKpiValor.setDataFormat(format.getFormat("#,##0.00"));
        estilos.put("kpiValor", estiloKpiValor);

        // 5. Desviación Negativa / Buena (Verde)
        CellStyle estiloDesvBuena = workbook.createCellStyle();
        estiloDesvBuena.setDataFormat(format.getFormat("#,##0.00"));
        Font fontVerde = workbook.createFont();
        fontVerde.setColor(IndexedColors.GREEN.getIndex());
        estiloDesvBuena.setFont(fontVerde);
        estilos.put("desviacionBuena", estiloDesvBuena);

        // 6. Desviación Positiva / Mala (Rojo)
        CellStyle estiloDesvMala = workbook.createCellStyle();
        estiloDesvMala.setDataFormat(format.getFormat("#,##0.00"));
        Font fontRoja = workbook.createFont();
        fontRoja.setColor(IndexedColors.RED.getIndex());
        estiloDesvMala.setFont(fontRoja);
        estilos.put("desviacionMala", estiloDesvMala);

        // Cabecera Azul Oscuro (Hoja Tareas)
        CellStyle estiloCabeceraAzul = workbook.createCellStyle();
        estiloCabeceraAzul.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estiloCabeceraAzul.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estiloCabeceraAzul.setAlignment(HorizontalAlignment.CENTER);
        estiloCabeceraAzul.setVerticalAlignment(VerticalAlignment.CENTER);
        Font fuenteCabeceraAzul = workbook.createFont();
        fuenteCabeceraAzul.setBold(true);
        fuenteCabeceraAzul.setColor(IndexedColors.WHITE.getIndex());
        estiloCabeceraAzul.setFont(fuenteCabeceraAzul);
        estilos.put("cabeceraAzul", estiloCabeceraAzul);

        // Estilo Fila Totales
        CellStyle estiloTotal = workbook.createCellStyle();
        estiloTotal.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        estiloTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estiloTotal.setAlignment(HorizontalAlignment.CENTER);
        Font fuenteTotal = workbook.createFont();
        fuenteTotal.setBold(true);
        estiloTotal.setFont(fuenteTotal);
        estiloTotal.setDataFormat(format.getFormat("#,##0.00"));
        estilos.put("filaTotal", estiloTotal);

        // Estilo Iconos Centrados
        CellStyle estiloIcono = workbook.createCellStyle();
        estiloIcono.setAlignment(HorizontalAlignment.CENTER);
        estiloIcono.setVerticalAlignment(VerticalAlignment.CENTER);
        estilos.put("icono", estiloIcono);

        return estilos;
    }

   private void generarHojaTareas(Workbook workbook, Map<String, CellStyle> estilos, Long idProyecto) {
        Sheet sheet = workbook.createSheet("2. TAREAS");
        
        // 1. CREACIÓN DE CABECERA (Azul Oscuro)
        String[] columnas = {
            "ID GITLAB", "FASE", "TAREA", "DEPARTAMENTO", 
            "EST. MÍN (h)", "EST. MÁX (h)", "HORAS REALES (h)", 
            "DESVIACIÓN (h)", "ESTADO SALUD", "ESTADO GITLAB"
        };
        
        Row filaCabecera = sheet.createRow(0);
        filaCabecera.setHeightInPoints(25);
        for (int i = 0; i < columnas.length; i++) {
            Cell celda = filaCabecera.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilos.get("cabeceraAzul"));
            sheet.setColumnWidth(i, 4500);
        }
        
        // 2. OBTENCIÓN DE DATOS Y VARIABLES PARA TOTALES
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto);
        double totalEstMin = 0.0, totalEstMax = 0.0, totalReales = 0.0, totalDesv = 0.0;
        
        // 3. POBLADO DE DATOS
        int rowNum = 1;
        for (FilaComparativaDTO tarea : tareas) {
            Row fila = sheet.createRow(rowNum++);
            
            fila.createCell(0).setCellValue(tarea.getIdGitlab() != null ? tarea.getIdGitlab() : "-");
            fila.createCell(1).setCellValue(tarea.getFase());
            fila.createCell(2).setCellValue(tarea.getTarea());
            fila.createCell(3).setCellValue(tarea.getDepartamento());
            
            Cell celdaEstMin = fila.createCell(4);
            celdaEstMin.setCellValue(tarea.getEstimacionMinima());
            celdaEstMin.setCellStyle(estilos.get("decimal"));
            totalEstMin += tarea.getEstimacionMinima();
            
            Cell celdaEstMax = fila.createCell(5);
            celdaEstMax.setCellValue(tarea.getEstimacionMaxima());
            celdaEstMax.setCellStyle(estilos.get("decimal"));
            totalEstMax += tarea.getEstimacionMaxima();
            
            Cell celdaReales = fila.createCell(6);
            celdaReales.setCellValue(tarea.getHorasReales());
            celdaReales.setCellStyle(estilos.get("decimal"));
            totalReales += tarea.getHorasReales();
            
            Cell celdaDesvHoras = fila.createCell(7);
            celdaDesvHoras.setCellValue(tarea.getDesviacionHoras());
            totalDesv += tarea.getDesviacionHoras();
            
            // Lógica de Semáforo y Formato Condicional
            String iconoSalud = "✅"; 
            if (tarea.getDesviacionHoras() > 0 && tarea.getDesviacionHoras() <= 10) {
                iconoSalud = "⚠️";
                celdaDesvHoras.setCellStyle(estilos.get("desviacionMala")); // Número Rojo
            } else if (tarea.getDesviacionHoras() > 10) {
                iconoSalud = "❌";
                celdaDesvHoras.setCellStyle(estilos.get("desviacionMala")); // Número Rojo
            } else {
                celdaDesvHoras.setCellStyle(estilos.get("desviacionBuena")); // Número Verde
            }
            
            Cell celdaSalud = fila.createCell(8);
            celdaSalud.setCellValue(iconoSalud);
            celdaSalud.setCellStyle(estilos.get("icono"));
            
            fila.createCell(9).setCellValue(tarea.getEstadoGitlab());
        }

        // 4. FILA DE TOTALES INFERIOR
        Row filaTotales = sheet.createRow(rowNum);
        filaTotales.setHeightInPoints(20);
        
        Cell celdaTextoTotal = filaTotales.createCell(0);
        celdaTextoTotal.setCellValue("TOTAL");
        celdaTextoTotal.setCellStyle(estilos.get("filaTotal"));
        for(int i=1; i<=3; i++) {
            Cell c = filaTotales.createCell(i);
            c.setCellStyle(estilos.get("filaTotal"));
        }
        
        Cell tMin = filaTotales.createCell(4); tMin.setCellValue(totalEstMin); tMin.setCellStyle(estilos.get("filaTotal"));
        Cell tMax = filaTotales.createCell(5); tMax.setCellValue(totalEstMax); tMax.setCellStyle(estilos.get("filaTotal"));
        Cell tReales = filaTotales.createCell(6); tReales.setCellValue(totalReales); tReales.setCellStyle(estilos.get("filaTotal"));
        Cell tDesv = filaTotales.createCell(7); tDesv.setCellValue(totalDesv); 
        
        // Estilo mixto para el Total Desviación
        CellStyle estiloTotalDesv = workbook.createCellStyle();
        estiloTotalDesv.cloneStyleFrom(estilos.get("filaTotal"));
        Font fontTotalDesv = workbook.createFont();
        fontTotalDesv.setBold(true);
        fontTotalDesv.setColor(totalDesv > 0 ? IndexedColors.RED.getIndex() : IndexedColors.GREEN.getIndex());
        estiloTotalDesv.setFont(fontTotalDesv);
        tDesv.setCellStyle(estiloTotalDesv);
        
        for(int i=8; i<=9; i++) {
            Cell c = filaTotales.createCell(i);
            c.setCellStyle(estilos.get("filaTotal"));
        }
    }

    private void escribirCeldaPorNombre(Workbook workbook, Sheet sheet, String nombreRango, Object valor) {
        Name nombre = workbook.getName(nombreRango);
        if (nombre != null) {
            // Encontrar la coordenada real a partir del nombre
            AreaReference ref = new AreaReference(nombre.getRefersToFormula(), workbook.getSpreadsheetVersion());
            CellReference cellRef = ref.getFirstCell();
            
            Row fila = sheet.getRow(cellRef.getRow());
            if (fila == null) {
                fila = sheet.createRow(cellRef.getRow());
            }
            
            Cell celda = fila.getCell(cellRef.getCol());
            if (celda == null) {
                celda = fila.createCell(cellRef.getCol());
            }
            
            // Escribir el valor respetando su tipo
            if (valor instanceof Double) {
                celda.setCellValue((Double) valor);
            } else if (valor instanceof Integer) {
                celda.setCellValue((Integer) valor);
            } else {
                celda.setCellValue(valor != null ? valor.toString() : "");
            }
        }
    }

    private void escribirListaDesdeAncla(Workbook workbook, Sheet sheet, String nombreRango, List<Object[]> datos) {
        Name nombre = workbook.getName(nombreRango);
        if (nombre == null) {
            return; // Si no encuentra el ancla, no hace nada
        }

        AreaReference ref = new AreaReference(nombre.getRefersToFormula(), workbook.getSpreadsheetVersion());
        CellReference ancla = ref.getFirstCell();
        int filaActual = ancla.getRow();
        int colInicial = ancla.getCol();

        for (Object[] filaDatos : datos) {
            Row row = sheet.getRow(filaActual);
            if (row == null) {
                row = sheet.createRow(filaActual);
            }

            for (int i = 0; i < filaDatos.length; i++) {
                Cell cell = row.getCell(colInicial + i);
                if (cell == null) {
                    cell = row.createCell(colInicial + i);
                }

                Object valor = filaDatos[i];
                if (valor instanceof Double) {
                    cell.setCellValue((Double) valor);
                } else if (valor instanceof Integer) {
                    cell.setCellValue((Integer) valor);
                } else if (valor instanceof Long) {
                    cell.setCellValue((Long) valor);
                } else {
                    cell.setCellValue(valor != null ? valor.toString() : "");
                }
            }
            filaActual++; // Bajamos una fila para el siguiente registro
        }
    }

    public ProblemasDetectadosDTO obtenerProblemasDetectados(Long idProyecto, Long idExcel){

        List<GitLabTarea> tareasBDINvalidas = gitLabTareaRepository.findByValidaAndTareaProyecto_IdProyecto(false, idProyecto);
        int numeroInvalidasGit = tareasBDINvalidas.size();

        List<ImputacionClockify> imputacionesBD = imputacionClockifyRepository.findByIdProyectoAndValida(idProyecto, false);

        int numeroInvalidasClockify = imputacionesBD.size();

        List<TareaProyecto> tareasBDSinImputaciones = tareaProyectoRepository.findTareasSinImputacionClockifyByProyecto(idProyecto);

        int numeroSinImputaciones = tareasBDSinImputaciones.size();

        return new ProblemasDetectadosDTO(numeroInvalidasGit, numeroInvalidasClockify, numeroSinImputaciones);
    }

}