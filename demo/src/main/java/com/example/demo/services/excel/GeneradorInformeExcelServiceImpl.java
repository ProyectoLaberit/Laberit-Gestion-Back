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

@Service("generadorInformeExcelService")
public class GeneradorInformeExcelServiceImpl implements GeneradorInformeExcelService {

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

            generarDashboardConPlantilla(workbook, estilos, idProyecto, idExcel);
            generarHojaTareas(workbook, estilos, idProyecto, idExcel);
            // ── CAMBIO: ahora pasamos estilos a generarHojaValidaciones ──
            generarHojaValidaciones(workbook, estilos, idProyecto);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte analítico Excel", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ESTILOS CORPORATIVOS
    // ─────────────────────────────────────────────────────────────────────────────

    private Map<String, CellStyle> crearEstilosCorporativos(Workbook workbook) {
        Map<String, CellStyle> estilos = new java.util.HashMap<>();
        DataFormat fmt = workbook.createDataFormat();

        // ── Paleta de colores Actualizada ────────────────────────────────────────────
        // Cabecera principal: gris oscuro → GREY_80_PERCENT
        // Fondo par: blanco #FFFFFF
        // Fondo impar: gris claro #F2F2F2 → GREY_25_PERCENT
        // Fondo totales: gris medio → GREY_40_PERCENT
        // Texto positivo: rojo #C00000
        // Texto negativo: verde #375623
        // ────────────────────────────────────────────────────────────────────────────

        // --- Fuentes reutilizables ---------------------------------------------------
        Font fBold = workbook.createFont();
        fBold.setBold(true);
        fBold.setFontName("Arial");
        fBold.setFontHeightInPoints((short) 10);

        Font fBoldWhite = workbook.createFont();
        fBoldWhite.setBold(true);
        fBoldWhite.setFontName("Arial");
        fBoldWhite.setFontHeightInPoints((short) 10);
        fBoldWhite.setColor(IndexedColors.WHITE.getIndex());

        Font fNormal = workbook.createFont();
        fNormal.setFontName("Arial");
        fNormal.setFontHeightInPoints((short) 10);

        Font fVerde = workbook.createFont();
        fVerde.setFontName("Arial");
        fVerde.setFontHeightInPoints((short) 10);
        fVerde.setBold(true);
        fVerde.setColor(IndexedColors.GREEN.getIndex());

        Font fRojo = workbook.createFont();
        fRojo.setFontName("Arial");
        fRojo.setFontHeightInPoints((short) 10);
        fRojo.setBold(true);
        fRojo.setColor(IndexedColors.RED.getIndex());

        Font fKpiBuena = workbook.createFont();
        fKpiBuena.setBold(true);
        fKpiBuena.setFontName("Arial");
        fKpiBuena.setFontHeightInPoints((short) 24);
        fKpiBuena.setColor(IndexedColors.GREEN.getIndex());

        Font fKpiMala = workbook.createFont();
        fKpiMala.setBold(true);
        fKpiMala.setFontName("Arial");
        fKpiMala.setFontHeightInPoints((short) 24);
        fKpiMala.setColor(IndexedColors.RED.getIndex());

        // --- 1. Cabecera principal (Gris oscuro + texto blanco) ----------------------
        CellStyle sCabecera = workbook.createCellStyle();
        sCabecera.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        sCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sCabecera.setAlignment(HorizontalAlignment.CENTER);
        sCabecera.setVerticalAlignment(VerticalAlignment.CENTER);
        sCabecera.setFont(fBoldWhite);
        aplicarBordes(sCabecera, BorderStyle.MEDIUM, IndexedColors.WHITE.getIndex());
        estilos.put("cabecera", sCabecera);
        estilos.put("cabeceraAzul", sCabecera); // Mantenemos el alias por compatibilidad

        // --- 2. Cabecera roja (dashboard KPI) ----------------------------------------
        CellStyle sCabeceraRoja = workbook.createCellStyle();
        sCabeceraRoja.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        sCabeceraRoja.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sCabeceraRoja.setAlignment(HorizontalAlignment.CENTER);
        sCabeceraRoja.setVerticalAlignment(VerticalAlignment.CENTER);
        sCabeceraRoja.setFont(fBoldWhite);
        aplicarBordes(sCabeceraRoja, BorderStyle.MEDIUM, IndexedColors.WHITE.getIndex());
        estilos.put("cabeceraRoja", sCabeceraRoja);

        // --- 3. KPI header (gris claro + negrita) ------------------------------------
        CellStyle sKpiHeader = workbook.createCellStyle();
        sKpiHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        sKpiHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sKpiHeader.setAlignment(HorizontalAlignment.CENTER);
        sKpiHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        sKpiHeader.setFont(fBold);
        aplicarBordes(sKpiHeader, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("kpiHeader", sKpiHeader);

        // --- 4. KPI valor (centrado con borde) ---------------------------------------
        CellStyle sKpiValor = workbook.createCellStyle();
        sKpiValor.setAlignment(HorizontalAlignment.CENTER);
        sKpiValor.setVerticalAlignment(VerticalAlignment.CENTER);
        sKpiValor.setFont(fBold);
        sKpiValor.setDataFormat(fmt.getFormat("#,##0"));
        aplicarBordes(sKpiValor, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("kpiValor", sKpiValor);

        // --- 5. Fila de dato estándar — texto (alineado izquierda) -------------------
        CellStyle sFilaTexto = workbook.createCellStyle();
        sFilaTexto.setAlignment(HorizontalAlignment.LEFT);
        sFilaTexto.setVerticalAlignment(VerticalAlignment.CENTER);
        sFilaTexto.setFont(fNormal);
        sFilaTexto.setWrapText(false);
        aplicarBordes(sFilaTexto, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("filaDatoTexto", sFilaTexto);

        // --- 6. Fila de dato estándar — número (alineado centro) --------------------
        CellStyle sFilaNum = workbook.createCellStyle();
        sFilaNum.setAlignment(HorizontalAlignment.CENTER);
        sFilaNum.setVerticalAlignment(VerticalAlignment.CENTER);
        sFilaNum.setFont(fNormal);
        sFilaNum.setDataFormat(fmt.getFormat("#,##0"));
        aplicarBordes(sFilaNum, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("filaDato", sFilaNum);

        // --- 7. Fila impar (zebra) — texto -------------------------------------------
        CellStyle sFilaTextoImpar = workbook.createCellStyle();
        sFilaTextoImpar.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        sFilaTextoImpar.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sFilaTextoImpar.setAlignment(HorizontalAlignment.LEFT);
        sFilaTextoImpar.setVerticalAlignment(VerticalAlignment.CENTER);
        sFilaTextoImpar.setFont(fNormal);
        sFilaTextoImpar.setWrapText(false);
        aplicarBordes(sFilaTextoImpar, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("filaDatoTextoImpar", sFilaTextoImpar);

        // --- 8. Fila impar (zebra) — número ------------------------------------------
        CellStyle sFilaNumImpar = workbook.createCellStyle();
        sFilaNumImpar.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        sFilaNumImpar.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sFilaNumImpar.setAlignment(HorizontalAlignment.CENTER);
        sFilaNumImpar.setVerticalAlignment(VerticalAlignment.CENTER);
        sFilaNumImpar.setFont(fNormal);
        sFilaNumImpar.setDataFormat(fmt.getFormat("#,##0"));
        aplicarBordes(sFilaNumImpar, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("filaDatoImpar", sFilaNumImpar);

        // --- 9. Fila totales (Gris medio + negrita) ----------------------------------
        CellStyle sTotal = workbook.createCellStyle();
        sTotal.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        sTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sTotal.setAlignment(HorizontalAlignment.CENTER);
        sTotal.setVerticalAlignment(VerticalAlignment.CENTER);
        sTotal.setFont(fBold);
        sTotal.setDataFormat(fmt.getFormat("#,##0"));
        aplicarBordes(sTotal, BorderStyle.MEDIUM, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("filaTotal", sTotal);

        // --- 10. Desviación buena — número verde con borde ---------------------------
        CellStyle sDesvBuena = workbook.createCellStyle();
        sDesvBuena.setAlignment(HorizontalAlignment.CENTER);
        sDesvBuena.setVerticalAlignment(VerticalAlignment.CENTER);
        sDesvBuena.setFont(fVerde);
        sDesvBuena.setDataFormat(fmt.getFormat("#,##0;-#,##0"));
        aplicarBordes(sDesvBuena, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("desviacionBuena", sDesvBuena);

        // --- 11. Desviación mala — número rojo con borde -----------------------------
        CellStyle sDesvMala = workbook.createCellStyle();
        sDesvMala.setAlignment(HorizontalAlignment.CENTER);
        sDesvMala.setVerticalAlignment(VerticalAlignment.CENTER);
        sDesvMala.setFont(fRojo);
        sDesvMala.setDataFormat(fmt.getFormat("+#,##0;-#,##0"));
        aplicarBordes(sDesvMala, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("desviacionMala", sDesvMala);

        // --- 12. KPI desviación grande buena (24pt verde) ----------------------------
        CellStyle sKpiBuena = workbook.createCellStyle();
        sKpiBuena.setAlignment(HorizontalAlignment.CENTER);
        sKpiBuena.setVerticalAlignment(VerticalAlignment.CENTER);
        sKpiBuena.setFont(fKpiBuena);
        estilos.put("kpiDesviacionBuena", sKpiBuena);

        // --- 13. KPI desviación grande mala (24pt rojo) ------------------------------
        CellStyle sKpiMala = workbook.createCellStyle();
        sKpiMala.setAlignment(HorizontalAlignment.CENTER);
        sKpiMala.setVerticalAlignment(VerticalAlignment.CENTER);
        sKpiMala.setFont(fKpiMala);
        estilos.put("kpiDesviacionMala", sKpiMala);

        // --- 14. Porcentaje verde ----------------------------------------------------
        CellStyle sPorcVerde = workbook.createCellStyle();
        sPorcVerde.setAlignment(HorizontalAlignment.CENTER);
        sPorcVerde.setVerticalAlignment(VerticalAlignment.CENTER);
        sPorcVerde.setDataFormat(fmt.getFormat("0%"));
        sPorcVerde.setFont(fVerde);
        aplicarBordes(sPorcVerde, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("porcentajeVerde", sPorcVerde);

        // --- 15. Porcentaje rojo -----------------------------------------------------
        CellStyle sPorcRojo = workbook.createCellStyle();
        sPorcRojo.setAlignment(HorizontalAlignment.CENTER);
        sPorcRojo.setVerticalAlignment(VerticalAlignment.CENTER);
        sPorcRojo.setDataFormat(fmt.getFormat("0%"));
        sPorcRojo.setFont(fRojo);
        aplicarBordes(sPorcRojo, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("porcentajeRojo", sPorcRojo);

        // --- 16. Decimal estándar (para celdas individuales) -------------------------
        CellStyle sDecimal = workbook.createCellStyle();
        sDecimal.setAlignment(HorizontalAlignment.CENTER);
        sDecimal.setDataFormat(fmt.getFormat("#,##0.00"));
        aplicarBordes(sDecimal, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("decimal", sDecimal);

        // --- 17. Icono centrado  -----------------------------------------------
        CellStyle sIcono = workbook.createCellStyle();
        sIcono.setAlignment(HorizontalAlignment.CENTER);
        sIcono.setVerticalAlignment(VerticalAlignment.CENTER);
        sIcono.setFont(fNormal);
        aplicarBordes(sIcono, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        estilos.put("icono", sIcono);

        return estilos;
    }

    /**
     * Aplica el mismo tipo de borde a los cuatro lados de un CellStyle.
     */
    private void aplicarBordes(CellStyle estilo, BorderStyle tipo, short color) {
        estilo.setBorderBottom(tipo);
        estilo.setBorderTop(tipo);
        estilo.setBorderLeft(tipo);
        estilo.setBorderRight(tipo);
        estilo.setBottomBorderColor(color);
        estilo.setTopBorderColor(color);
        estilo.setLeftBorderColor(color);
        estilo.setRightBorderColor(color);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // GENERACIÓN DE HOJAS
    // ─────────────────────────────────────────────────────────────────────────────

    private void generarDashboardConPlantilla(Workbook workbook, Map<String, CellStyle> estilos, Long idProyecto,
            Integer idExcel) {
        Sheet sheet = workbook.getSheetAt(0);

        // 1. KPIs Principales
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

        // 3. Tareas
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

        if (tareas != null && !tareas.isEmpty()) {

            // 4. Top 10 Desviaciones
            List<Object[]> top10 = tareas.stream()
                    .sorted((t1, t2) -> Double.compare(t2.getDesviacionHoras(), t1.getDesviacionHoras()))
                    .limit(10)
                    .map(t -> new Object[] {
                            t.getFase() != null ? t.getFase() : "",
                            t.getTarea() != null ? t.getTarea() : "",
                            Math.round(t.getEstimacionMaxima()),
                            Math.round(t.getHorasReales()),
                            Math.round(t.getDesviacionHoras())
                    })
                    .collect(Collectors.toList());

            // ── CAMBIO: pasamos estilos en todas las llamadas ──
            escribirListaDesdeAncla(workbook, sheet, "TOP10_INICIO", top10, estilos);

            List<Object[]> datosDepartamentos = obtenerDatosGraficoDepartamentos(idProyecto, idExcel);
            escribirListaDesdeAncla(workbook, sheet, "GRAF_DEP_INICIO", datosDepartamentos, estilos);

            List<Object[]> datosFases = obtenerDatosGraficoFases(idProyecto, idExcel);
            escribirListaDesdeAncla(workbook, sheet, "GRAF_FASE_INICIO", datosFases, estilos);
        }
    }

    private void generarHojaTareas(Workbook workbook, Map<String, CellStyle> estilos, Long idProyecto,
            Integer idExcel) {
        Sheet sheet = workbook.getSheetAt(1);

        List<FilaGitLabTareaDTO> tareas = obtenerTareasEstructuradas(idProyecto, idExcel);

        List<Object[]> filasTabla = tareas.stream()
                .map(tarea -> {
                    String estadoOriginal = tarea.getEstadoGitlab() != null ? tarea.getEstadoGitlab().toLowerCase()
                            : "";
                    String estadoTraducido = "No vinculada";
                    if (estadoOriginal.equals("opened"))
                        estadoTraducido = "En proceso";
                    else if (estadoOriginal.equals("closed"))
                        estadoTraducido = "Completada";

                    String idFormateado = "-";
                    if (tarea.getIdGitlab() != null && !tarea.getIdGitlab().trim().isEmpty()
                            && !tarea.getIdGitlab().equals("-")) {
                        idFormateado = "#" + tarea.getIdGitlab();
                    }

                    return new Object[] {
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

        // ── CAMBIO: pasamos estilos ──
        escribirListaDesdeAncla(workbook, sheet, "TABLA_TAREAS_INICIO", filasTabla, estilos);

        double totalEstMin = 0.0, totalEstMax = 0.0, totalReales = 0.0, totalDesv = 0.0;
        for (FilaGitLabTareaDTO tarea : tareas) {
            totalEstMin += tarea.getEstimacionMinima();
            totalEstMax += tarea.getEstimacionMaxima();
            totalReales += tarea.getHorasReales();
            totalDesv += tarea.getDesviacionHoras();
        }

        double porcentajeTotal = (totalEstMax > 0) ? totalDesv / totalEstMax : 0.0;

        escribirCeldaPorNombre(workbook, sheet, "TOTAL_MIN", totalEstMin);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_MAX", totalEstMax);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_REALES", totalReales);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_DESV", totalDesv);
        escribirCeldaPorNombre(workbook, sheet, "TOTAL_DESV_PORC", porcentajeTotal);
    }

    // ── CAMBIO: firma actualizada para recibir estilos ──
    private void generarHojaValidaciones(Workbook workbook, Map<String, CellStyle> estilos, Long idProyecto) {
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
        List<FilaValidacionGitlabDTO> tareasGitlab = obtenerFilasValidacionGitlab(idProyecto);
        if (tareasGitlab != null && !tareasGitlab.isEmpty()) {
            List<Object[]> filasGitlab = tareasGitlab.stream().map(fila -> {
                String textoEstado = "-";
                if (fila.getTareaInternaCompletada() != null) {
                    textoEstado = fila.getTareaInternaCompletada() ? "Terminada" : "En proceso";
                }
                String iconoVinculada = fila.isVinculada() ? "✅" : "❌";

                return new Object[] {
                        fila.getIdGitlab() != null ? fila.getIdGitlab() : "-",
                        fila.getNombreGitlab() != null ? fila.getNombreGitlab() : "",
                        fila.getNombreTareaProyecto() != null ? fila.getNombreTareaProyecto() : "",
                        textoEstado,
                        iconoVinculada
                };
            }).collect(Collectors.toList());

            // ── CAMBIO: pasamos estilos ──
            escribirListaDesdeAncla(workbook, sheet, "TABLA_VAL_GITLAB_INICIO", filasGitlab, estilos, false);
        }

        // 3. Tabla de Auditoría Clockify
        List<FilaAuditoriaClockifyDTO> erroresClockify = obtenerFilasAuditoriaClockify(idProyecto);
        if (erroresClockify != null && !erroresClockify.isEmpty()) {
            List<Object[]> filasClockify = erroresClockify.stream().map(fila -> new Object[] {
                    fila.getFecha() != null ? fila.getFecha() : "",
                    fila.getDescripcion() != null ? fila.getDescripcion() : "",
                    fila.getHoras() != null ? fila.getHoras() : ""
            }).collect(Collectors.toList());

            // ── CAMBIO: pasamos estilos ──
            escribirListaDesdeAncla(workbook, sheet, "TABLA_AUD_CLOCKIFY_INICIO", filasClockify, estilos, false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ESCRITURA DE CELDAS
    // ─────────────────────────────────────────────────────────────────────────────

    private void escribirCeldaPorNombre(Workbook workbook, Sheet sheet, String nombreRango, Object valor) {
        Name nombre = workbook.getName(nombreRango);
        if (nombre == null)
            return;

        AreaReference ref = new AreaReference(nombre.getRefersToFormula(), workbook.getSpreadsheetVersion());
        CellReference cellRef = ref.getFirstCell();

        Row fila = sheet.getRow(cellRef.getRow());
        if (fila == null)
            fila = sheet.createRow(cellRef.getRow());

        Cell celda = fila.getCell(cellRef.getCol());
        if (celda == null)
            celda = fila.createCell(cellRef.getCol());

        if (valor instanceof Double)
            celda.setCellValue((Double) valor);
        else if (valor instanceof Integer)
            celda.setCellValue((Integer) valor);
        else if (valor instanceof Long)
            celda.setCellValue((Long) valor);
        else
            celda.setCellValue(valor != null ? valor.toString() : "");
    }

    /**
     * Escribe una lista de filas a partir de un rango nombrado como ancla.
     * Aplica estilos zebra (filas alternas) y distingue texto de número
     * para asignar la alineación correcta.
     *
     * Columnas consideradas "texto" según su tipo en tiempo de ejecución:
     * - String → alineación izquierda
     * - Número → alineación central + formato #,##0
     */
    // ── CAMBIO: nueva firma con parámetro estilos ──
    private void escribirListaDesdeAncla(Workbook workbook, Sheet sheet, String nombreRango,
            List<Object[]> datos, Map<String, CellStyle> estilos) {
        escribirListaDesdeAncla(workbook, sheet, nombreRango, datos, estilos, true);
    }

    private void escribirListaDesdeAncla(Workbook workbook, Sheet sheet, String nombreRango,
            List<Object[]> datos, Map<String, CellStyle> estilos, boolean usarZebra) {

        Name nombre = workbook.getName(nombreRango);
        if (nombre == null || datos == null || datos.isEmpty())
            return;

        AreaReference ref = new AreaReference(nombre.getRefersToFormula(), workbook.getSpreadsheetVersion());
        CellReference ancla = ref.getFirstCell();
        int filaInicio = ancla.getRow();
        int colInicial = ancla.getCol();
        int filaActual = filaInicio;

        for (int idx = 0; idx < datos.size(); idx++) {
            Object[] filaDatos = datos.get(idx);
            boolean esImpar = usarZebra && (idx % 2 != 0); // zebra: filas alternas con fondo gris

            Row row = sheet.getRow(filaActual);
            if (row == null)
                row = sheet.createRow(filaActual);

            for (int i = 0; i < filaDatos.length; i++) {
                Object valor = filaDatos[i];

                Cell cell = row.getCell(colInicial + i);
                if (cell == null)
                    cell = row.createCell(colInicial + i);

                // ── Asignar estilo según tipo de dato y paridad de fila ──
                if (estilos != null) {
                    CellStyle estilo = resolverEstilo(valor, esImpar, estilos);
                    cell.setCellStyle(estilo);
                }

                // ── Escribir valor ──
                if (valor instanceof Double)
                    cell.setCellValue((Double) valor);
                else if (valor instanceof Integer)
                    cell.setCellValue((Integer) valor);
                else if (valor instanceof Long)
                    cell.setCellValue((Long) valor);
                else
                    cell.setCellValue(valor != null ? valor.toString() : "");
            }
            filaActual++;
        }
    }

    /**
     * Devuelve el CellStyle apropiado según si el valor es texto o número
     * y si la fila es par o impar (zebra striping).
     */
    private CellStyle resolverEstilo(Object valor, boolean esImpar, Map<String, CellStyle> estilos) {
        boolean esTexto = (valor instanceof String);
        if (esTexto) {
            return estilos.getOrDefault(esImpar ? "filaDatoTextoImpar" : "filaDatoTexto",
                    estilos.get("filaDatoTexto"));
        } else {
            return estilos.getOrDefault(esImpar ? "filaDatoImpar" : "filaDato",
                    estilos.get("filaDato"));
        }
    }

    // Mantenemos el helper que sigue usando algunas llamadas internas
    private void escribirCelda(Sheet sheet, int row, int col, double valor, CellStyle estilo) {
        Row fila = sheet.getRow(row);
        if (fila == null)
            fila = sheet.createRow(row);
        Cell celda = fila.getCell(col);
        if (celda == null)
            celda = fila.createCell(col);
        celda.setCellValue(valor);
        if (estilo != null)
            celda.setCellStyle(estilo);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // LÓGICA DE NEGOCIO (sin cambios respecto al original)
    // ─────────────────────────────────────────────────────────────────────────────

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
        Double min = detalleEstimacionRepository.obtenerTotalHorasMinimasProyecto(idProyecto, idExcel);
        Double max = detalleEstimacionRepository.obtenerTotalHorasMaximasProyecto(idProyecto, idExcel);
        Double reales = imputacionClockifyRepository.sumarHorasTotalesProyecto(idProyecto);

        long horasMin = (min != null) ? Math.round(min) : 0L;
        long horasMax = (max != null) ? Math.round(max) : 0L;
        long horasReales = (reales != null) ? Math.round(reales) : 0L;
        long desviacion = horasReales - horasMax;

        int imputacionesInvalidas = imputacionClockifyRepository.countByIdProyectoAndValidaFalse(idProyecto);
        int totalTareas = tareaProyectoRepository.countByIdProyecto(idProyecto);
        int tareasVinculadasGitlab = gitLabTareaRepository.contarTareasVinculadasPorProyecto(idProyecto);

        int porcentajeGitlab = 0;
        if (totalTareas > 0) {
            porcentajeGitlab = (int) Math.round(((double) tareasVinculadasGitlab / totalTareas) * 100);
        }

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
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

        List<Object[]> todos = tareas.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDepartamento() != null ? t.getDepartamento() : "Sin Departamento",
                        Collectors.summingDouble(FilaComparativaDTO::getHorasReales)))
                .entrySet().stream()
                .map(e -> new Object[] { e.getKey(), Math.round(e.getValue()) })
                .sorted((e1, e2) -> Long.compare((Long) e2[1], (Long) e1[1]))
                .collect(Collectors.toList());

        if (todos.size() <= 4)
            return todos;

        List<Object[]> top4 = new java.util.ArrayList<>(todos.subList(0, 4));
        long sumaOtros = todos.subList(4, todos.size()).stream().mapToLong(e -> (Long) e[1]).sum();
        top4.add(new Object[] { "Otros", sumaOtros });
        return top4;
    }

    private List<Object[]> obtenerDatosGraficoFases(Long idProyecto, Integer idExcel) {
        List<FilaComparativaDTO> tareas = tareaProyectoRepository.obtenerComparativaTareas(idProyecto).stream()
                .filter(t -> t.getIdExcel() != null && t.getIdExcel().equals(idExcel))
                .collect(Collectors.toList());

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

        return tareas.stream().map(t -> {
            FilaGitLabTareaDTO dto = new FilaGitLabTareaDTO();
            dto.setIdGitlab(t.getIdGitlab() != null ? t.getIdGitlab() : "-");
            dto.setFase(t.getFase());
            dto.setTarea(t.getTarea());
            dto.setDepartamento(t.getDepartamento());
            dto.setEstimacionMinima(t.getEstimacionMinima() != null ? (int) Math.round(t.getEstimacionMinima()) : 0);
            dto.setEstimacionMaxima(t.getEstimacionMaxima() != null ? (int) Math.round(t.getEstimacionMaxima()) : 0);
            dto.setHorasReales(t.getHorasReales() != null ? (int) Math.round(t.getHorasReales()) : 0);
            dto.setDesviacionHoras(t.getDesviacionHoras() != null ? (int) Math.round(t.getDesviacionHoras()) : 0);
            dto.setDesviacionPorcentaje(t.getDesviacionPorcentaje() != null ? t.getDesviacionPorcentaje() : 0.0);
            dto.setEstadoGitlab(t.getEstadoGitlab() != null ? t.getEstadoGitlab() : "-");
            return dto;
        }).collect(Collectors.toList());
    }

    public List<FilaValidacionGitlabDTO> obtenerFilasValidacionGitlab(Long idProyecto) {
        List<GitLabTarea> tareas = gitLabTareaRepository.findByIdProyecto(idProyecto);

        return tareas.stream().map(t -> {
            FilaValidacionGitlabDTO fila = new FilaValidacionGitlabDTO();
            fila.setIdGitlab(t.getNumeroGitlab() != null ? "#" + t.getNumeroGitlab() : "-");
            fila.setNombreGitlab(t.getTitulo());
            fila.setVinculada(t.getValida());

            Long idTareaInterna = t.getTareaProyecto();
            if (idTareaInterna != null) {
                TareaProyecto tareaInterna = tareaProyectoRepository.findById(idTareaInterna).orElse(null);
                if (tareaInterna != null) {
                    fila.setNombreTareaProyecto(tareaInterna.getTarea());
                    fila.setTareaInternaCompletada(tareaInterna.getCompletada());
                } else {
                    fila.setNombreTareaProyecto("-");
                    fila.setTareaInternaCompletada(null);
                }
            } else {
                fila.setNombreTareaProyecto("-");
                fila.setTareaInternaCompletada(null);
            }
            return fila;
        }).collect(Collectors.toList());
    }

    @Override
    public List<FilaAuditoriaClockifyDTO> obtenerFilasAuditoriaClockify(Long idProyecto) {
        List<ImputacionClockify> imputaciones = imputacionClockifyRepository.findByIdProyectoAndValida(idProyecto,
                false);

        return imputaciones.stream().map(i -> {
            FilaAuditoriaClockifyDTO fila = new FilaAuditoriaClockifyDTO();
            fila.setFecha(FilaAuditoriaClockifyDTO.formatearFecha(i.getFecha()));
            fila.setDescripcion(i.getDescripcionOriginal());
            fila.setHoras(FilaAuditoriaClockifyDTO.formatearHoras(i.getHorasTrabajadas()));
            return fila;
        }).collect(Collectors.toList());
    }

    @Override
    public ResumenValidacionDTO obtenerResumenValidacion(Long idProyecto) {
        ResumenValidacionDTO resumen = new ResumenValidacionDTO();

        List<GitLabTarea> tareasGitlab = gitLabTareaRepository.findByIdProyecto(idProyecto);
        List<GitLabTarea> okGitlabLista = gitLabTareaRepository.findByValidaAndIdProyecto(true, idProyecto);
        List<GitLabTarea> huerfanasGitlab = gitLabTareaRepository.findByValidaAndIdProyecto(false, idProyecto);

        resumen.setTotalTareasGitlab(tareasGitlab != null ? tareasGitlab.size() : 0);
        resumen.setTareasGitlabOk(okGitlabLista != null ? okGitlabLista.size() : 0);
        resumen.setTareasGitlabHuerfanas(huerfanasGitlab != null ? huerfanasGitlab.size() : 0);

        List<ImputacionClockify> imputacionesClockify = imputacionClockifyRepository.findByIdProyecto(idProyecto);
        List<ImputacionClockify> okClockify = imputacionClockifyRepository.findByIdProyectoAndValida(idProyecto, true);
        List<ImputacionClockify> erroneasClockify = imputacionClockifyRepository.findByIdProyectoAndValida(idProyecto,
                false);

        resumen.setTotalImputacionesClockify(imputacionesClockify != null ? imputacionesClockify.size() : 0);
        resumen.setImputacionesClockifyOk(okClockify != null ? okClockify.size() : 0);
        resumen.setImputacionesClockifyErroneas(erroneasClockify != null ? erroneasClockify.size() : 0);

        return resumen;
    }
}
