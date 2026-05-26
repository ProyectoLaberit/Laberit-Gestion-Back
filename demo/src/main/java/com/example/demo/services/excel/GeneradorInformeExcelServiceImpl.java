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
import com.example.demo.repository.GitLabTareaRepository;
import com.example.demo.repository.ImputacionClockifyRepository;
import com.example.demo.repository.TareaProyectoRepository;

import ch.qos.logback.classic.Logger;

import com.example.demo.dto.excel.CabeceraDTO;
import com.example.demo.dto.excel.FilaComparativaDTO;
import com.example.demo.dto.excel.ProblemasDetectadosDTO;

import org.apache.poi.xssf.usermodel.XSSFColor;
import org.slf4j.LoggerFactory;

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

    

   
    // Aquí sí podemos usar @Autowired (o private final con Lombok)
    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    @Autowired
    private ImputacionClockifyRepository imputacionClockifyRepository;

    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

    @Autowired 
    private GitLabTareaRepository gitLabTareaRepository;

    

 @Override
public ByteArrayInputStream generarExcelAnalitico(Long idProyecto, Integer idExcel) {
    try (InputStream is = getClass().getResourceAsStream("/plantillas/dashboard_template.xlsx");
         Workbook workbook = WorkbookFactory.create(is); 
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

        Map<String, CellStyle> estilos = crearEstilosCorporativos(workbook);

        // Llamamos al método con el nombre correcto
        generarDashboardConPlantilla(workbook,  idProyecto, idExcel);
        generarHojaTareas(workbook, estilos, idProyecto);

        workbook.write(out);
        return new ByteArrayInputStream(out.toByteArray());

    } catch (Exception e) {
        throw new RuntimeException("Error al generar el reporte analítico Excel", e);
    }
}

    // Aquí sí podemos tener métodos privados con lógica
// 1. Modificamos la firma para incluir Integer idExcel
   private void generarDashboardConPlantilla(Workbook workbook, Long idProyecto, Integer idExcel) {
    Sheet sheet = workbook.getSheetAt(0);

    // 1. KPIs Principales usando el DTO
    CabeceraDTO cabecera = obtenerDatosCabecera(idProyecto, idExcel);
    if (cabecera != null) {
        escribirCeldaPorNombre(workbook, sheet, "KPI_MINIMAS", cabecera.getHorasMinimas());
        escribirCeldaPorNombre(workbook, sheet, "KPI_MAXIMAS", cabecera.getHorasMaximas());
        escribirCeldaPorNombre(workbook, sheet, "KPI_REALES", cabecera.getHorasReales());
        escribirCeldaPorNombre(workbook, sheet, "KPI_DESVIACION", cabecera.getDesviacion());
        escribirCeldaPorNombre(workbook, sheet, "KPI_VALIDAS_GITLAB", cabecera.getPorcentajesValidasGitlab());
        escribirCeldaPorNombre(workbook, sheet, "KPI_INVALIDAS_CLOCKIFY", cabecera.getImputacionesInvalidadas());
    }

    // 2. KPIs de Problemas Detectados (Incidencias)
    ProblemasDetectadosDTO problemas = obtenerProblemasDetectados(idProyecto, idExcel);
    System.out.println("Problemas Detectados: " + problemas.getTareasGitlabNoReconocidas()); // Log para depuración
    if (problemas != null) {
        escribirCeldaPorNombre(workbook, sheet, "INC_GITLAB", problemas.getTareasGitlabNoReconocidas());
        escribirCeldaPorNombre(workbook, sheet, "INC_CLOCKIFY", problemas.getImputacionesInvalidas());
        escribirCeldaPorNombre(workbook, sheet, "INC_SIN_HORAS", problemas.getTareasSinHoras());
    }

    // 3. Obtener TODAS las tareas para extraer Top 10 y Gráficos
    List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto);

    // 4. Validar que la lista no esté vacía antes de procesar streams
    if (tareas != null && !tareas.isEmpty()) {
        
        // 4.1. Procesar y rellenar TOP 10 Desviaciones
        List<Object[]> top10 = tareas.stream()
                .sorted((t1, t2) -> Double.compare(t2.getDesviacionHoras(), t1.getDesviacionHoras()))
                .limit(10)
                .map(t -> new Object[]{
                        t.getFase(),
                        t.getTarea(),
                        Math.round(t.getEstimacionMaxima()),
                        Math.round(t.getHorasReales()),
                        Math.round(t.getDesviacionHoras())
                })
                .collect(Collectors.toList());
        
        escribirListaDesdeAncla(workbook, sheet, "TOP10_INICIO", top10);

        // 4.2. Procesar y rellenar Gráfico de Departamentos (Ordenado)
        List<Object[]> datosDepartamentos = tareas.stream()
                .collect(Collectors.groupingBy(FilaComparativaDTO::getDepartamento, Collectors.summingDouble(FilaComparativaDTO::getHorasReales)))
                .entrySet().stream()
                .map(e -> new Object[]{e.getKey(), Math.round(e.getValue())})
                .sorted((e1, e2) -> Long.compare((Long) e2[1], (Long) e1[1]))
                .collect(Collectors.toList());
                
        escribirListaDesdeAncla(workbook, sheet, "GRAF_DEP_INICIO", datosDepartamentos);

        // 4.3. Procesar y rellenar Gráfico de Fases (Ordenado)
        List<Object[]> datosFases = tareas.stream()
                .collect(Collectors.groupingBy(FilaComparativaDTO::getFase, Collectors.summingDouble(FilaComparativaDTO::getHorasReales)))
                .entrySet().stream()
                .map(e -> new Object[]{e.getKey(), Math.round(e.getValue())})
                .sorted((e1, e2) -> Long.compare((Long) e2[1], (Long) e1[1]))
                .collect(Collectors.toList());
                
        escribirListaDesdeAncla(workbook, sheet, "GRAF_FASE_INICIO", datosFases);
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
        // 1. OBTENER LA HOJA EXISTENTE DE LA PLANTILLA (Índice 1 es la segunda hoja)
        Sheet sheet = workbook.getSheetAt(1);
    
        
        // 2. OBTENCIÓN DE DATOS Y VARIABLES PARA TOTALES
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto);
        double totalEstMin = 0.0;
        double totalEstMax = 0.0;
        double totalReales = 0.0;
        double totalDesv = 0.0;
        
        // 3. POBLADO DE DATOS
        int rowNum = 2; // Empezamos en la fila 2 para no pisar la cabecera visual
        
        for (FilaComparativaDTO tarea : tareas) {
            // 3.1. Obtener o crear la fila de forma segura
            Row fila = sheet.getRow(rowNum);
            if (fila == null) {
                fila = sheet.createRow(rowNum);
            }

            // 3.2. Insertar datos de texto (con prevención de nulos)
            fila.createCell(0).setCellValue(tarea.getIdGitlab() != null ? tarea.getIdGitlab() : "-");
            fila.createCell(1).setCellValue(tarea.getFase() != null ? tarea.getFase() : "");
            fila.createCell(2).setCellValue(tarea.getTarea() != null ? tarea.getTarea() : "");
            fila.createCell(3).setCellValue(tarea.getDepartamento() != null ? tarea.getDepartamento() : "");

            // 3.3. Insertar datos numéricos y acumular totales
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

            // 3.4. Lógica de Semáforo y Formato Condicional
            String iconoSalud = "✅"; 
            
            if (tarea.getDesviacionHoras() > 0 && tarea.getDesviacionHoras() <= 10) {
                iconoSalud = "⚠️";
                celdaDesvHoras.setCellStyle(estilos.get("desviacionMala"));
            } else if (tarea.getDesviacionHoras() > 10) {
                iconoSalud = "❌";
                celdaDesvHoras.setCellStyle(estilos.get("desviacionMala"));
            } else {
                celdaDesvHoras.setCellStyle(estilos.get("desviacionBuena"));
            }

            Cell celdaSalud = fila.createCell(8);
            celdaSalud.setCellValue(iconoSalud);
            celdaSalud.setCellStyle(estilos.get("icono"));

            fila.createCell(9).setCellValue(tarea.getEstadoGitlab() != null ? tarea.getEstadoGitlab() : "");

            // 3.5. Incrementar el contador para la siguiente vuelta
            rowNum++;
        }

        // 4. FILA DE TOTALES INFERIOR
        Row filaTotales = sheet.getRow(rowNum);
        if (filaTotales == null) {
            filaTotales = sheet.createRow(rowNum);
        }
        filaTotales.setHeightInPoints(20);
        
        Cell celdaTextoTotal = filaTotales.createCell(0);
        celdaTextoTotal.setCellValue("TOTAL");
        celdaTextoTotal.setCellStyle(estilos.get("filaTotal"));
        
        for (int i = 1; i <= 3; i++) {
            Cell c = filaTotales.createCell(i);
            c.setCellStyle(estilos.get("filaTotal"));
        }
        
        Cell tMin = filaTotales.createCell(4); 
        tMin.setCellValue(totalEstMin); 
        tMin.setCellStyle(estilos.get("filaTotal"));
        
        Cell tMax = filaTotales.createCell(5); 
        tMax.setCellValue(totalEstMax); 
        tMax.setCellStyle(estilos.get("filaTotal"));
        
        Cell tReales = filaTotales.createCell(6); 
        tReales.setCellValue(totalReales); 
        tReales.setCellStyle(estilos.get("filaTotal"));
        
        Cell tDesv = filaTotales.createCell(7); 
        tDesv.setCellValue(totalDesv); 
        
        // Estilo mixto para el Total Desviación
        CellStyle estiloTotalDesv = workbook.createCellStyle();
        estiloTotalDesv.cloneStyleFrom(estilos.get("filaTotal"));
        Font fontTotalDesv = workbook.createFont();
        fontTotalDesv.setBold(true);
        
        if (totalDesv > 0) {
            fontTotalDesv.setColor(IndexedColors.RED.getIndex());
        } else {
            fontTotalDesv.setColor(IndexedColors.GREEN.getIndex());
        }
        
        estiloTotalDesv.setFont(fontTotalDesv);
        tDesv.setCellStyle(estiloTotalDesv);
        
        for (int i = 8; i <= 9; i++) {
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

    public ProblemasDetectadosDTO obtenerProblemasDetectados(Long idProyecto, int idExcel){

        List<GitLabTarea> tareasBDInvalidas = gitLabTareaRepository.findByValidaAndIdProyecto(false, idProyecto);
        int numeroInvalidasGit = tareasBDInvalidas.size();

        List<ImputacionClockify> imputacionesBD = imputacionClockifyRepository.findByIdProyectoAndValida(idProyecto, false);

        int numeroInvalidasClockify = imputacionesBD.size();

        List<TareaProyecto> tareasBDSinImputaciones = tareaProyectoRepository.findTareasSinImputacionClockifyByProyecto(idProyecto);

        int numeroSinImputaciones = tareasBDSinImputaciones.size();

        return new ProblemasDetectadosDTO(numeroInvalidasGit, numeroInvalidasClockify, numeroSinImputaciones);
    }

@Override
public CabeceraDTO obtenerDatosCabecera(Long idProyecto, Integer idExcel) {
    // 1. Recuperar datos brutos de los repositorios
    Double min = detalleEstimacionRepository.obtenerTotalHorasMinimasProyecto(idProyecto, idExcel);
    Double max = detalleEstimacionRepository.obtenerTotalHorasMaximasProyecto(idProyecto, idExcel);
    
    // NOTA: Asumimos que Clockify suma las horas a nivel de proyecto, si depende del excel, añade el parámetro también.
    Double reales = imputacionClockifyRepository.sumarHorasTotalesProyecto(idProyecto);
    
    // 2. Controlar nulos antes de redondear
    long horasMin = (min != null) ? Math.round(min) : 0L;
    long horasMax = (max != null) ? Math.round(max) : 0L;
    long horasReales = (reales != null) ? Math.round(reales) : 0L;
    long desviacion = horasReales - horasMax;

    // 3. Consultas para los contadores de GitLab y Clockify (Provisional)
    int imputacionesInvalidas = imputacionClockifyRepository.countByIdProyectoAndValidaFalse(idProyecto);
    
    int totalTareas = tareaProyectoRepository.countByIdProyecto(idProyecto);
     int tareasVinculadasGitlab = gitLabTareaRepository.contarTareasVinculadasPorProyecto(idProyecto); 

    int porcentajeGitlab = 0;
    if (totalTareas > 0) {
        porcentajeGitlab = (int) Math.round(((double) tareasVinculadasGitlab / totalTareas) * 100);
    }

    // Trazas de depuración (Logs)
   

   // 4. Trazas de depuración con prints normales
    System.out.println("--- DEBUG KPIs EXCEL ---");
    System.out.println("Total Tareas Proyecto: " + totalTareas);
    System.out.println("Tareas en GitLab: " + tareasVinculadasGitlab);
    System.out.println("Porcentaje GitLab Calculado: " + porcentajeGitlab + "%");
    System.out.println("Imputaciones Inválidas: " + imputacionesInvalidas);
    System.out.println("------------------------");

    // 4. Construir y retornar el DTO
    CabeceraDTO cabecera = new CabeceraDTO();
    cabecera.setHorasMinimas(horasMin);
    cabecera.setHorasMaximas(horasMax);
    cabecera.setHorasReales(horasReales);
    cabecera.setDesviacion(desviacion);
    cabecera.setPorcentajesValidasGitlab(porcentajeGitlab);
    cabecera.setImputacionesInvalidadas(imputacionesInvalidas);

    return cabecera;
}

}