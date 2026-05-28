package com.example.demo.services.excel;

import com.example.demo.repository.GitLabTareaRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.GitLabTarea;
import com.example.demo.entity.ImputacionClockify;
import com.example.demo.entity.TareaProyecto;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.repository.GitLabTareaRepository;
import com.example.demo.repository.ImputacionClockifyRepository;
import com.example.demo.repository.ProyectoRepository;
import com.example.demo.repository.TareaProyectoRepository;

import ch.qos.logback.classic.Logger;

import com.example.demo.dto.excel.CabeceraDTO;
import com.example.demo.dto.excel.FilaAuditoriaClockifyDTO;
import com.example.demo.dto.excel.FilaComparativaDTO;
import com.example.demo.dto.excel.FilaGitLabTareaDTO;
import com.example.demo.dto.excel.FilaValidacionGitlabDTO;
import com.example.demo.dto.excel.ProblemasDetectadosDTO;
import com.example.demo.dto.excel.ResumenValidacionDTO;

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

    // Aquí sí podemos usar @Autowired
    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    @Autowired
    private com.example.demo.services.DetalleEstimacionService detalleEstimacionService;

    @Autowired
    private ImputacionClockifyRepository imputacionClockifyRepository;

    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

    @Autowired
    private GitLabTareaRepository gitLabTareaRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Override
    public ByteArrayInputStream generarExcelAnalitico(Long idProyecto, Integer idExcel) {
        try (InputStream is = getClass().getResourceAsStream("/plantillas/dashboard_template.xlsx");
                Workbook workbook = WorkbookFactory.create(is);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Map<String, CellStyle> estilos = crearEstilosCorporativos(workbook);

            // Llamamos al método con el nombre correcto
            generarDashboardConPlantilla(workbook, estilos, idProyecto, idExcel);
            generarHojaTareas(workbook, estilos, idProyecto, idExcel);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte analítico Excel", e);
        }
    }

    // Aquí sí podemos tener métodos privados con lógica
    // Modificamos la firma para incluir Integer idExcel
    private void generarDashboardConPlantilla(Workbook workbook, Map<String, CellStyle> estilos, Long idProyecto, Integer idExcel) {
        Sheet sheet = workbook.getSheetAt(0);

        // KPIs Principales
        CabeceraDTO cabecera = obtenerDatosCabecera(idProyecto, idExcel);
        if (cabecera != null) {
            escribirCeldaPorNombre(workbook, sheet, "KPI_MINIMAS", cabecera.getHorasMinimas());
            escribirCeldaPorNombre(workbook, sheet, "KPI_MAXIMAS", cabecera.getHorasMaximas());
            escribirCeldaPorNombre(workbook, sheet, "KPI_REALES", cabecera.getHorasReales());
            escribirCeldaPorNombre(workbook, sheet, "KPI_DESVIACION", cabecera.getDesviacion());
            escribirCeldaPorNombre(workbook, sheet, "KPI_VALIDAS_GITLAB", cabecera.getPorcentajesValidasGitlab());
            escribirCeldaPorNombre(workbook, sheet, "KPI_INVALIDAS_CLOCKIFY", cabecera.getImputacionesInvalidadas());
        }

        // KPIs de Problemas Detectados (Incidencias)
        ProblemasDetectadosDTO problemas = obtenerProblemasDetectados(idProyecto, idExcel);
        if (problemas != null) {
            escribirCeldaPorNombre(workbook, sheet, "INC_GITLAB", problemas.getTareasGitlabNoReconocidas());
            escribirCeldaPorNombre(workbook, sheet, "INC_CLOCKIFY", problemas.getImputacionesInvalidas());
            escribirCeldaPorNombre(workbook, sheet, "INC_SIN_HORAS", problemas.getTareasSinHoras());
        }

        // Obtener TODAS las tareas para extraer Top 10 y Gráficos
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

        if (tareas != null && !tareas.isEmpty()) {

            // Top 10 Desviaciones
            List<Object[]> top10 = tareas.stream()
                    .sorted((t1, t2) -> Double.compare(t2.getDesviacionHoras(), t1.getDesviacionHoras()))
                    .limit(10)
                    .map(t -> {
                        return new Object[]{
                            t.getFase() != null ? t.getFase() : "",
                            t.getTarea() != null ? t.getTarea() : "",
                            Math.round(t.getEstimacionMaxima()),
                            Math.round(t.getHorasReales()),
                            Math.round(t.getDesviacionHoras())
                        };
                    })
                    .collect(Collectors.toList());

            escribirListaDesdeAncla(workbook, sheet, "TOP10_INICIO", top10);

            // Aplicar colores a la columna de Desviación del TOP 10
            Name anclaTop10 = workbook.getName("TOP10_INICIO");
            if (anclaTop10 != null) {
                org.apache.poi.ss.util.AreaReference ref = new org.apache.poi.ss.util.AreaReference(anclaTop10.getRefersToFormula(), workbook.getSpreadsheetVersion());
                int filaInicial = ref.getFirstCell().getRow();
                int colDesviacion = ref.getFirstCell().getCol() + 4; // Columna 5 (índice 4)

                for (int i = 0; i < top10.size(); i++) {
                    Row fila = sheet.getRow(filaInicial + i);
                    if (fila != null) {
                        Cell celdaDesv = fila.getCell(colDesviacion);
                        if (celdaDesv != null) {
                            double valorDesv = celdaDesv.getNumericCellValue();
                            if (valorDesv > 0) {
                                celdaDesv.setCellStyle(estilos.get("desviacionMala"));
                            } else {
                                celdaDesv.setCellStyle(estilos.get("desviacionBuena"));
                            }
                        }
                    }
                }
            }

            // Procesar y rellenar Gráfico de Departamentos
            List<Object[]> datosDepartamentos = obtenerDatosGraficoDepartamentos(idProyecto, idExcel);
            escribirListaDesdeAncla(workbook, sheet, "GRAF_DEP_INICIO", datosDepartamentos);

            // Gráfico de Fases
            List<Object[]> datosFases = obtenerDatosGraficoFases(idProyecto, idExcel);
            escribirListaDesdeAncla(workbook, sheet, "GRAF_FASE_INICIO", datosFases);
        }
    }

    private void escribirCelda(Sheet sheet, int row, int col, double valor, CellStyle estilo) {
        Row fila = sheet.getRow(row);
        if (fila == null)
            fila = sheet.createRow(row);

        Cell celda = fila.getCell(col);
        if (celda == null)
            celda = fila.createCell(col);

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

    private void generarHojaTareas(Workbook workbook, Map<String, CellStyle> estilos, Long idProyecto,
            Integer idExcel) {
        // 1. OBTENER LA HOJA EXISTENTE DE LA PLANTILLA (Índice 1 es la segunda hoja)
        Sheet sheet = workbook.getSheetAt(1);

        // 2. OBTENCIÓN DE DATOS Y VARIABLES PARA TOTALES
        List<FilaGitLabTareaDTO> tareas = obtenerTareasEstructuradas(idProyecto, idExcel);

        List<Object[]> filasTabla = tareas.stream()
        .map(tarea -> {
            return new Object[]{
                tarea.getIdGitlab(),
                tarea.getFase() != null ? tarea.getFase() : "",
                tarea.getTarea() != null ? tarea.getTarea() : "",
                tarea.getDepartamento() != null ? tarea.getDepartamento() : "",
                tarea.getEstimacionMinima(),
                tarea.getEstimacionMaxima(),
                tarea.getHorasReales(),
                tarea.getDesviacionHoras(),
                tarea.getEstadoGitlab()
            };
        })
        .collect(Collectors.toList());

        escribirListaDesdeAncla(workbook, sheet, "TABLA_TAREAS_INICIO", filasTabla);

        double totalEstMin = 0.0;
        double totalEstMax = 0.0;
        double totalReales = 0.0;
        double totalDesv = 0.0;
        
        for (FilaGitLabTareaDTO tarea : tareas) {
            totalEstMin += tarea.getEstimacionMinima();
            totalEstMax += tarea.getEstimacionMaxima();
            totalReales += tarea.getHorasReales();
            totalDesv += tarea.getDesviacionHoras();
        }

        // 4. FILA DE TOTALES INFERIOR
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_MIN", totalEstMin);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_MAX", totalEstMax);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_REALES", totalReales);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_DESV", totalDesv);
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

    public ProblemasDetectadosDTO obtenerProblemasDetectados(Long idProyecto, int idExcel) {

        List<GitLabTarea> tareasBDInvalidas = gitLabTareaRepository.findByValidaAndIdProyecto_Id(false, idProyecto);
        int numeroInvalidasGit = tareasBDInvalidas.size();

        List<ImputacionClockify> imputacionesBD = imputacionClockifyRepository.findByIdProyectoAndValida(idProyecto,
                false);

        int numeroInvalidasClockify = imputacionesBD.size();

        List<TareaProyecto> tareasBDSinImputaciones = tareaProyectoRepository
                .findTareasSinImputacionClockifyByProyecto(idProyecto);

        int numeroSinImputaciones = tareasBDSinImputaciones.size();

        return new ProblemasDetectadosDTO(numeroInvalidasGit, numeroInvalidasClockify, numeroSinImputaciones);
    }

    @Override
    public CabeceraDTO obtenerDatosCabecera(Long idProyecto, Integer idExcel) {
        // 1. Recuperar datos brutos de los repositorios
        Double min = detalleEstimacionRepository.obtenerTotalHorasMinimasProyecto(idProyecto, idExcel);
        Double max = detalleEstimacionRepository.obtenerTotalHorasMaximasProyecto(idProyecto, idExcel);

        // NOTA: Asumimos que Clockify suma las horas a nivel de proyecto, si depende
        // del excel, añade el parámetro también.
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

    private List<Object[]> obtenerDatosGraficoDepartamentos(Long idProyecto, Integer idExcel) {
        // Filtra las tareas del proyecto para conservar únicamente las asociadas al
        // Excel indicado
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

        // Agrupa por departamento, suma las horas reales y ordena el resultado de mayor
        // a menor
        List<Object[]> todosLosDepartamentos = tareas.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDepartamento() != null ? t.getDepartamento() : "Sin Departamento",
                        Collectors.summingDouble(FilaComparativaDTO::getHorasReales)))
                .entrySet().stream()
                .map(e -> new Object[] { e.getKey(), Math.round(e.getValue()) })
                .sorted((e1, e2) -> Long.compare((Long) e2[1], (Long) e1[1]))
                .collect(Collectors.toList());

        // Devuelve la lista original si contiene 4 elementos o menos
        if (todosLosDepartamentos.size() <= 4) {
            return todosLosDepartamentos;
        }

        // Separa los 4 departamentos principales y agrupa la suma del resto bajo la
        // categoría "Otros"
        List<Object[]> top4 = new java.util.ArrayList<>(todosLosDepartamentos.subList(0, 4));
        long sumaOtros = todosLosDepartamentos.subList(4, todosLosDepartamentos.size()).stream()
                .mapToLong(e -> (Long) e[1])
                .sum();

        top4.add(new Object[] { "Otros", sumaOtros });
        return top4;
    }

    private List<Object[]> obtenerDatosGraficoFases(Long idProyecto, Integer idExcel) {
        // Filtra las tareas del proyecto para conservar únicamente las asociadas al
        // Excel indicado
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

        // Agrupa por fase, suma las horas reales, redondea los valores y los ordena de
        // mayor a menor
        return tareas.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getFase() != null ? t.getFase() : "Sin Fase",
                        Collectors.summingDouble(FilaComparativaDTO::getHorasReales)))
                .entrySet().stream()
                .map(e -> new Object[] { e.getKey(), Math.round(e.getValue()) })
                .sorted((e1, e2) -> Long.compare((Long) e2[1], (Long) e1[1]))
                .collect(Collectors.toList());
    }

    @Override
    public List<FilaGitLabTareaDTO> obtenerTareasEstructuradas(Long idProyecto, Integer idExcel) {
        // Filtra las tareas del proyecto para conservar únicamente las asociadas al Excel indicado
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

        // Mapea la lista de entidades procesadas hacia la estructura del nuevo DTO
        return tareas.stream()
                .map(t -> {
                    FilaGitLabTareaDTO dto = new FilaGitLabTareaDTO();
                    dto.setIdGitlab(t.getIdGitlab() != null ? t.getIdGitlab() : "-");
                    dto.setFase(t.getFase());
                    dto.setTarea(t.getTarea());
                    dto.setDepartamento(t.getDepartamento());
                    dto.setEstimacionMinima((int) Math.round(t.getEstimacionMinima()));
                    dto.setEstimacionMaxima((int) Math.round(t.getEstimacionMaxima()));
                    dto.setHorasReales((int) Math.round(t.getHorasReales()));
                    dto.setDesviacionHoras((int) Math.round(t.getDesviacionHoras()));
                    dto.setEstadoGitlab(t.getEstadoGitlab() != null ? t.getEstadoGitlab() : "-");
                    return dto;
                })
                .collect(Collectors.toList());}
    public ResumenValidacionDTO obtenerResumenValidacion(Long idProyecto) {

        // --- GitLab ---
        int totalGitlab = gitLabTareaRepository.findByIdProyecto(
                proyectoRepository.findById(idProyecto).orElseThrow()).size();

        // Cambiamos esto: usamos findByValidaAndIdProyecto_Id en lugar de
        // contarTareasVinculadasPorProyecto
        int gitlabOk = gitLabTareaRepository.findByValidaAndIdProyecto_Id(true, idProyecto).size();
        int gitlabHuerfanas = totalGitlab - gitlabOk;

        // --- Clockify ---
        int totalClockify = imputacionClockifyRepository.findByIdProyecto(idProyecto).size();
        int clockifyErroneas = imputacionClockifyRepository.countByIdProyectoAndValidaFalse(idProyecto);
        int clockifyOk = totalClockify - clockifyErroneas;

        return new ResumenValidacionDTO(
                totalGitlab, gitlabOk, gitlabHuerfanas,
                totalClockify, clockifyOk, clockifyErroneas);
    }

    public List<FilaValidacionGitlabDTO> obtenerFilasValidacionGitlab(Long idProyecto) {

        List<GitLabTarea> tareas = gitLabTareaRepository.findByIdProyecto(
                proyectoRepository.findById(idProyecto).orElseThrow());

        return tareas.stream().map(t -> {
            FilaValidacionGitlabDTO fila = new FilaValidacionGitlabDTO();
            fila.setIdGitlab(t.getIssueId());
            fila.setNombreGitlab(t.getTitulo());
            fila.setNombreProyecto(
                    t.getTareaProyecto() != null ? t.getTareaProyecto().getTarea() : "No vinculada");
            fila.setEstado(t.getEstado());
            fila.setVinculada(t.getValida());
            return fila;
        }).collect(Collectors.toList());
    }

    @Override
    public List<FilaAuditoriaClockifyDTO> obtenerFilasAuditoriaClockify(Long idProyecto) {

        List<ImputacionClockify> imputaciones = imputacionClockifyRepository
                .findByIdProyectoAndValida(idProyecto, false);

        return imputaciones.stream().map(i -> {
            FilaAuditoriaClockifyDTO fila = new FilaAuditoriaClockifyDTO();
            fila.setFecha(FilaAuditoriaClockifyDTO.formatearFecha(i.getFecha()));
            fila.setDescripcion(i.getDescripcionOriginal());
            fila.setHoras(FilaAuditoriaClockifyDTO.formatearHoras(i.getHorasTrabajadas()));
            return fila;
        }).collect(Collectors.toList());
    }

}
