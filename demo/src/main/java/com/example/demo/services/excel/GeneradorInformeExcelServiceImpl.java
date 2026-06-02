package com.example.demo.services.excel;

import com.example.demo.repository.GitLabTareaRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.entity.GitLabTarea;
import com.example.demo.entity.ImputacionClockify;
import com.example.demo.entity.TareaProyecto;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.repository.ImputacionClockifyRepository;
import com.example.demo.repository.ProyectoRepository;
import com.example.demo.repository.TareaProyectoRepository;
import com.example.demo.dto.excel.CabeceraDTO;
import com.example.demo.dto.excel.FilaAuditoriaClockifyDTO;
import com.example.demo.dto.excel.FilaComparativaDTO;
import com.example.demo.dto.excel.FilaGitLabTareaDTO;
import com.example.demo.dto.excel.FilaValidacionGitlabDTO;
import com.example.demo.dto.excel.ProblemasDetectadosDTO;
import com.example.demo.dto.excel.ResumenValidacionDTO;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.*;
import java.io.InputStream;
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
             generarHojaValidaciones(workbook, idProyecto);       
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

        // 1. KPIs Principales (Sin forzar estilos desde Java)
        CabeceraDTO cabecera = obtenerDatosCabecera(idProyecto, idExcel);
        if (cabecera != null) {
            escribirCeldaPorNombre(workbook, sheet, "KPI_MINIMAS", cabecera.getHorasMinimas());
            escribirCeldaPorNombre(workbook, sheet, "KPI_MAXIMAS", cabecera.getHorasMaximas());
            escribirCeldaPorNombre(workbook, sheet, "KPI_REALES", cabecera.getHorasReales());
            escribirCeldaPorNombre(workbook, sheet, "KPI_DESVIACION", cabecera.getDesviacion());
            escribirCeldaPorNombre(workbook, sheet, "KPI_VALIDAS_GITLAB", cabecera.getPorcentajesValidasGitlab());
            escribirCeldaPorNombre(workbook, sheet, "KPI_INVALIDAS_CLOCKIFY", cabecera.getImputacionesInvalidadas());
        }

        // 2. KPIs de Problemas Detectados
        ProblemasDetectadosDTO problemas = obtenerProblemasDetectados(idProyecto, idExcel);
        if (problemas != null) {
            escribirCeldaPorNombre(workbook, sheet, "INC_GITLAB", problemas.getTareasGitlabNoReconocidas());
            escribirCeldaPorNombre(workbook, sheet, "INC_CLOCKIFY", problemas.getImputacionesInvalidas());
            escribirCeldaPorNombre(workbook, sheet, "INC_SIN_HORAS", problemas.getTareasSinHoras());
        }

        // 3. Obtener TODAS las tareas
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

        if (tareas != null && !tareas.isEmpty()) {

            // 4. Top 10 Desviaciones (Solo inyectar datos, sin estilos de color)
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

            // 5. Procesar Gráficos
            List<Object[]> datosDepartamentos = obtenerDatosGraficoDepartamentos(idProyecto, idExcel);
            escribirListaDesdeAncla(workbook, sheet, "GRAF_DEP_INICIO", datosDepartamentos);

            List<Object[]> datosFases = obtenerDatosGraficoFases(idProyecto, idExcel);
            escribirListaDesdeAncla(workbook, sheet, "GRAF_FASE_INICIO", datosFases);
        }
    }

    private void escribirCelda(Sheet sheet, int row, int col, double valor, CellStyle estilo) {
        Row fila = sheet.getRow(row);
        if (fila == null){
            fila = sheet.createRow(row);
        }
            
 
        Cell celda = fila.getCell(col);
        if (celda == null){
            celda = fila.createCell(col);
        }
            

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
        estiloDesvBuena.setDataFormat(format.getFormat("-#,##0.00")); 
        estiloDesvBuena.setAlignment(HorizontalAlignment.CENTER);
        estiloDesvBuena.setVerticalAlignment(VerticalAlignment.CENTER);
        estiloDesvBuena.setBorderBottom(BorderStyle.THIN);
        estiloDesvBuena.setBorderTop(BorderStyle.THIN);
        estiloDesvBuena.setBorderLeft(BorderStyle.THIN);
        estiloDesvBuena.setBorderRight(BorderStyle.THIN);
        Font fontVerde = workbook.createFont();
        fontVerde.setColor(IndexedColors.GREEN.getIndex());
        estiloDesvBuena.setFont(fontVerde);
        estilos.put("desviacionBuena", estiloDesvBuena);

        // 6. Desviación Positiva / Mala (Rojo con +)
        CellStyle estiloDesvMala = workbook.createCellStyle();
        estiloDesvMala.setDataFormat(format.getFormat("+#,##0.00")); // El + fuerza el símbolo
        estiloDesvMala.setAlignment(HorizontalAlignment.CENTER);
        estiloDesvMala.setVerticalAlignment(VerticalAlignment.CENTER);
        estiloDesvMala.setBorderBottom(BorderStyle.THIN);
        estiloDesvMala.setBorderTop(BorderStyle.THIN);
        estiloDesvMala.setBorderLeft(BorderStyle.THIN);
        estiloDesvMala.setBorderRight(BorderStyle.THIN);
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

        // Añade esto al final de crearEstilosCorporativos:
        Font fuenteKpiBuena = workbook.createFont();
        fuenteKpiBuena.setColor(IndexedColors.GREEN.getIndex());
        fuenteKpiBuena.setBold(true);
        fuenteKpiBuena.setFontHeightInPoints((short) 24); // Ajusta al tamaño de tu KPI

        Font fuenteKpiMala = workbook.createFont();
        fuenteKpiMala.setColor(IndexedColors.RED.getIndex());
        fuenteKpiMala.setBold(true);
        fuenteKpiMala.setFontHeightInPoints((short) 24);

        CellStyle kpiBuena = workbook.createCellStyle();
        kpiBuena.setAlignment(HorizontalAlignment.CENTER);
        kpiBuena.setVerticalAlignment(VerticalAlignment.CENTER);
        kpiBuena.setFont(fuenteKpiBuena);
        estilos.put("kpiDesviacionBuena", kpiBuena);

        CellStyle kpiMala = workbook.createCellStyle();
        kpiMala.setAlignment(HorizontalAlignment.CENTER);
        kpiMala.setVerticalAlignment(VerticalAlignment.CENTER);
        kpiMala.setFont(fuenteKpiMala);
        estilos.put("kpiDesviacionMala", kpiMala);

        return estilos;
    }

  private void generarHojaTareas(Workbook workbook, Map<String, CellStyle> estilos, Long idProyecto, Integer idExcel) {
        Sheet sheet = workbook.getSheetAt(1);

        List<FilaGitLabTareaDTO> tareas = obtenerTareasEstructuradas(idProyecto, idExcel);

        List<Object[]> filasTabla = tareas.stream()
            .map(tarea -> {
                String estadoOriginal = tarea.getEstadoGitlab() != null ? tarea.getEstadoGitlab().toLowerCase() : "";
                String estadoTraducido = "Sin issue";
                
                if (estadoOriginal.equals("opened")) {
                    estadoTraducido = "En proceso";
                } else {
                    if (estadoOriginal.equals("closed")) {
                        estadoTraducido = "Completada";
                    }
                }

                String idFormateado = "-";
                if (tarea.getIdGitlab() != null && !tarea.getIdGitlab().trim().isEmpty() && !tarea.getIdGitlab().equals("-")) {
                    idFormateado = "#" + tarea.getIdGitlab();
                }

                return new Object[]{
                    idFormateado,
                    tarea.getFase() != null ? tarea.getFase() : "",
                    tarea.getTarea() != null ? tarea.getTarea() : "",
                    tarea.getDepartamento() != null ? tarea.getDepartamento() : "",
                    tarea.getEstimacionMinima(),
                    tarea.getEstimacionMaxima(),
                    tarea.getHorasReales(),
                    tarea.getDesviacionHoras(),
                    tarea.getDesviacionPorcentaje(),
                    estadoTraducido                  
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

        double porcentajeTotal = 0.0;
        if (totalEstMax > 0) {
            porcentajeTotal = (totalDesv / totalEstMax) * 100;
        }

        escribirCeldaPorNombre(workbook, sheet, "TOTAL_MIN", totalEstMin);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_MAX", totalEstMax);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_REALES", totalReales);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_DESV", totalDesv);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_DESV_PORC", porcentajeTotal);
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

            // Escribir el valor respetando su tipo exacto (Añadido soporte para Long)
            if (valor instanceof Double) {
                celda.setCellValue((Double) valor);
            } else if (valor instanceof Integer) {
                celda.setCellValue((Integer) valor);
            } else if (valor instanceof Long) {
                celda.setCellValue((Long) valor);
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

        List<GitLabTarea> tareasBDInvalidas = gitLabTareaRepository.findByValidaAndIdProyecto(false, idProyecto);
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
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

        return tareas.stream()
                .map(t -> {
                    FilaGitLabTareaDTO dto = new FilaGitLabTareaDTO();
                    dto.setIdGitlab(t.getIdGitlab() != null ? t.getIdGitlab() : "-");
                    dto.setFase(t.getFase());
                    dto.setTarea(t.getTarea());
                    dto.setDepartamento(t.getDepartamento());

                    if (t.getEstimacionMinima() != null) {
                        dto.setEstimacionMinima((int) Math.round(t.getEstimacionMinima()));
                    } else {
                        dto.setEstimacionMinima(0);
                    }

                    if (t.getEstimacionMaxima() != null) {
                        dto.setEstimacionMaxima((int) Math.round(t.getEstimacionMaxima()));
                    } else {
                        dto.setEstimacionMaxima(0);
                    }

                    if (t.getHorasReales() != null) {
                        dto.setHorasReales((int) Math.round(t.getHorasReales()));
                    } else {
                        dto.setHorasReales(0);
                    }

                    if (t.getDesviacionHoras() != null) {
                        dto.setDesviacionHoras((int) Math.round(t.getDesviacionHoras()));
                    } else {
                        dto.setDesviacionHoras(0);
                    }

                    if (t.getDesviacionPorcentaje() != null) {
                        dto.setDesviacionPorcentaje(t.getDesviacionPorcentaje());
                    } else {
                        dto.setDesviacionPorcentaje(0.0);
                    }

                    dto.setEstadoGitlab(t.getEstadoGitlab() != null ? t.getEstadoGitlab() : "-");
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<FilaValidacionGitlabDTO> obtenerFilasValidacionGitlab(Long idProyecto) {

        List<GitLabTarea> tareas = gitLabTareaRepository.findByIdProyecto(idProyecto);
        return tareas.stream().map(t -> {
            FilaValidacionGitlabDTO fila = new FilaValidacionGitlabDTO();
            fila.setIdGitlab(t.getIssueId());
            fila.setNombreGitlab(t.getTitulo());
            fila.setNombreProyecto(
                    t.getIdProyecto() != null ? proyectoRepository.findById(t.getIdProyecto()).get().getNombre() : "No vinculada");
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

    private void generarHojaValidaciones(Workbook workbook, Long idProyecto) {
    Sheet sheet = workbook.getSheetAt(2);

    // 1. KPIs Resumen Superior
    ResumenValidacionDTO resumen = obtenerResumenValidacion(idProyecto);
    if (resumen != null) {
        escribirCeldaPorNombre(workbook, sheet, "RES_GIT_TOTAL", resumen.getTotalTareasGitlab());
        escribirCeldaPorNombre(workbook, sheet, "RES_GIT_OK", resumen.getTareasGitlabOk());
        escribirCeldaPorNombre(workbook, sheet, "RES_GIT_HUERFANAS", resumen.getTareasGitlabHuerfanas());
        escribirCeldaPorNombre(workbook, sheet, "RES_CLK_TOTAL", resumen.getTotalImputacionesClockify());
        escribirCeldaPorNombre(workbook, sheet, "RES_CLK_OK", resumen.getImputacionesClockifyOk());
        escribirCeldaPorNombre(workbook, sheet, "RES_CLK_ERRONEAS", resumen.getImputacionesClockifyErroneas());
    }

    // 2. Tabla de Validación GitLab
    Name anclaGitlab = workbook.getName("TABLA_VAL_GITLAB_INICIO");
    if (anclaGitlab != null) {
        org.apache.poi.ss.util.AreaReference refGit = new org.apache.poi.ss.util.AreaReference(anclaGitlab.getRefersToFormula(), workbook.getSpreadsheetVersion());
        org.apache.poi.ss.util.CellReference cellRefGit = refGit.getFirstCell();
        int rowNumGit = cellRefGit.getRow();
        int colStartGit = cellRefGit.getCol();

        List<FilaValidacionGitlabDTO> tareasGitlab = obtenerFilasValidacionGitlab(idProyecto);
        
        if (tareasGitlab != null) {
            for (FilaValidacionGitlabDTO fila : tareasGitlab) {
                Row row = sheet.getRow(rowNumGit);
                if (row == null) {
                    row = sheet.createRow(rowNumGit);
                }
                
                row.createCell(colStartGit + 0).setCellValue(fila.getIdGitlab() != null ? fila.getIdGitlab() : "-");
                row.createCell(colStartGit + 1).setCellValue(fila.getNombreGitlab() != null ? fila.getNombreGitlab() : "");
                row.createCell(colStartGit + 2).setCellValue(fila.getNombreProyecto() != null ? fila.getNombreProyecto() : "");
                row.createCell(colStartGit + 3).setCellValue(fila.getEstado() != null ? fila.getEstado() : "");
                
                String iconoVinculada = "❌";
                if (fila.isVinculada()) {
                    iconoVinculada = "✅";
                }
                row.createCell(colStartGit + 4).setCellValue(iconoVinculada);
                
                rowNumGit++;
            }
        }
    }

    // 3. Tabla de Auditoría Clockify
    Name anclaClockify = workbook.getName("TABLA_AUD_CLOCKIFY_INICIO");
    if (anclaClockify != null) {
        org.apache.poi.ss.util.AreaReference refClk = new org.apache.poi.ss.util.AreaReference(anclaClockify.getRefersToFormula(), workbook.getSpreadsheetVersion());
        org.apache.poi.ss.util.CellReference cellRefClk = refClk.getFirstCell();
        int rowNumClk = cellRefClk.getRow();
        int colStartClk = cellRefClk.getCol();

        List<FilaAuditoriaClockifyDTO> erroresClockify = obtenerFilasAuditoriaClockify(idProyecto);
        
        if (erroresClockify != null) {
            for (FilaAuditoriaClockifyDTO fila : erroresClockify) {
                Row row = sheet.getRow(rowNumClk);
                if (row == null) {
                    row = sheet.createRow(rowNumClk);
                }
                
                row.createCell(colStartClk + 0).setCellValue(fila.getFecha() != null ? fila.getFecha() : "");
                row.createCell(colStartClk + 1).setCellValue(fila.getDescripcion() != null ? fila.getDescripcion() : "");
                row.createCell(colStartClk + 2).setCellValue(fila.getHoras() != null ? fila.getHoras() : "");
                
                rowNumClk++;
            }
        }
    }
}

    @Override
    public ResumenValidacionDTO obtenerResumenValidacion(Long idProyecto) {
        List<GitLabTarea> tareasGitlab = gitLabTareaRepository.findTodasByProyectoIncluyendoVinculacion(idProyecto);
        int totalGitlab = tareasGitlab != null ? tareasGitlab.size() : 0;
        int gitlabOk = tareasGitlab != null
                ? (int) tareasGitlab.stream().filter(t -> Boolean.TRUE.equals(t.getValida())).count()
                : 0;
        int gitlabHuerfanas = totalGitlab - gitlabOk;

        List<ImputacionClockify> imputacionesClockify = imputacionClockifyRepository.findByIdProyecto(idProyecto);
        int totalClockify = imputacionesClockify != null ? imputacionesClockify.size() : 0;
        int clockifyOk = imputacionesClockify != null
                ? (int) imputacionesClockify.stream().filter(i -> Boolean.TRUE.equals(i.getValida())).count()
                : 0;
        int clockifyErroneas = totalClockify - clockifyOk;

        return new ResumenValidacionDTO(
                totalGitlab, gitlabOk, gitlabHuerfanas,
                totalClockify, clockifyOk, clockifyErroneas);
    }
}
