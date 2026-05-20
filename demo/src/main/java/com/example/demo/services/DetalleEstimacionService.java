package com.example.demo.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.annotation.Auditable;
import com.example.demo.dto.DetalleEstimacionDTO;
import com.example.demo.dto.ResumenTiemposDTO;
import com.example.demo.dto.TareaSubfaseDTO;
import com.example.demo.entity.Departamento;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.entity.Fase;
import com.example.demo.entity.RangoDepartamento;
import com.example.demo.entity.TareaProyecto;
import com.example.demo.repository.DepartamentoRepository;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.repository.FaseRepository;
import com.example.demo.repository.TareaProyectoRepository;
import com.example.demo.repository.ExcelRepository;
import com.example.demo.repository.ImputacionClockifyRepository;

@Service
public class DetalleEstimacionService {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;
    
    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private FaseRepository faseRepository;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private ExcelRepository excelRepository;

    @Autowired
    private ImputacionClockifyRepository imputacionClockifyRepository;

    /**
     * Procesa un archivo Excel físico, implementa la estrategia Find-or-Create en el 
     * pivote TareaProyecto y guarda los presupuestos de estimación numéricos puros en BD.
     * @param archivo El archivo MultipartFile subido desde el cliente.
     * @param proyectoId ID del proyecto al que pertenece la estimación.
     * @param usuarioId ID del usuario que sube el archivo.
     * @return El número total de presupuestos de tareas insertadas en la base de datos.
     */
   @Auditable(
        accion = "IMPORTAR_EXCEL", 
        tabla = "excel", 
        entidad = Excel.class,
        descripcion = "Se importó un nuevo documento Excel para el proyecto con ID: #{#proyectoId}"
    )
    public int procesarExcel(MultipartFile archivo, long proyectoId, Integer usuarioId) throws Exception {
        System.out.println("--- DIAGNÓSTICO PROFUNDO DE EXCEL ---");
        try {
            Excel registroExcel = new Excel();
            registroExcel.setIdProyecto(proyectoId);
            registroExcel.setIdUsuario(usuarioId);
            registroExcel.setFechaSubida(LocalDate.now());
            registroExcel.setRutaArchivo("uploads/" + archivo.getOriginalFilename());
            registroExcel.setVigente(true);
            
            Excel excelGuardado = excelService.guardarDatosExcel(registroExcel);
            Integer idExcelGenerado = excelGuardado.getIdExcel();

            List<DetalleEstimacion> listaParaGuardar = new ArrayList<>();
            Workbook workbook = WorkbookFactory.create(archivo.getInputStream());
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();

            List<Fase> todasLasFasesBD = faseRepository.findAll();
            System.out.println("Fases encontradas en BD: " + todasLasFasesBD.size());

            List<RangoDepartamento> mapaColumnas = Arrays.asList(
                new RangoDepartamento(3, 4, "Comercial"), new RangoDepartamento(5, 6, "Direccion"),
                new RangoDepartamento(7, 8, "back"), new RangoDepartamento(9, 10, "front"),
                new RangoDepartamento(11, 12, "soporte"), new RangoDepartamento(13, 14, "mk"),
                new RangoDepartamento(15, 16, "ux"), new RangoDepartamento(17, 18, "ui"),
                new RangoDepartamento(19, 20, "wp-maq")
            );

            for (RangoDepartamento rango : mapaColumnas) {
                rango.setIdBD(determinarDepartamento(rango.getNombreExcel()));
                System.out.println("Depto Excel: " + rango.getNombreExcel() + " -> ID BD: " + rango.getIdBD());
            }

            Sheet hoja = workbook.getSheetAt(0);
            Integer idFasePadreActual = null;
            Integer idSubfaseActual = null;
            String nombreTareaActual = null; 

            for (Row fila : hoja) {
                if (fila.getRowNum() < 4) { 
                    continue; 
                }
                if (esFilaFinal(fila)) { 
                    break; 
                }

                String valFase = formatter.formatCellValue(fila.getCell(0));
                String faseLimpia = normalizarTexto(valFase);
                if (!faseLimpia.isEmpty()) {
                    idFasePadreActual = todasLasFasesBD.stream()
                        .filter(f -> { return f.getFasePadre() == null && normalizarTexto(f.getNombre()).equals(faseLimpia); })
                        .map(Fase::getId)
                        .findFirst()
                        .orElse(null);
                    idSubfaseActual = null; 
                    nombreTareaActual = null; 
                }

                String valSubfase = formatter.formatCellValue(fila.getCell(1));
                String subfaseLimpia = normalizarTexto(valSubfase);
                if (!subfaseLimpia.isEmpty() && idFasePadreActual != null) {
                    final Integer idPadre = idFasePadreActual;
                    idSubfaseActual = todasLasFasesBD.stream()
                        .filter(f -> { return f.getFasePadre() != null && f.getFasePadre().equals(idPadre) && normalizarTexto(f.getNombre()).equals(subfaseLimpia); })
                        .map(Fase::getId)
                        .findFirst()
                        .orElse(null);
                    nombreTareaActual = null; 
                }

                String valTarea = formatter.formatCellValue(fila.getCell(2)).trim();
                if (!valTarea.isEmpty() && !normalizarTexto(valTarea).equals("analisis")) {
                    nombreTareaActual = valTarea;
                } else {
                    nombreTareaActual = null;
                }

                if (fila.getRowNum() >= 4 && fila.getRowNum() <= 8) {
                    System.out.println("Fila " + fila.getRowNum() + " -> Padre ID: " + idFasePadreActual + " | Subfase ID: " + idSubfaseActual + " | Tarea: " + nombreTareaActual);
                }

                if (idSubfaseActual != null && nombreTareaActual != null) {
                    for (RangoDepartamento depto : mapaColumnas) {
                        if (depto.getIdBD() == null || depto.getIdBD() == -1) {
                            continue;
                        }

                        Double min = extraerNumeroDeCelda(fila.getCell(depto.getColMin()), evaluator);
                        Double max = extraerNumeroDeCelda(fila.getCell(depto.getColMax()), evaluator);

                        if ((min != null && min > 0) || (max != null && max > 0)) {
                            final Integer idDepto = depto.getIdBD();
                            final Integer idSubfase = idSubfaseActual;
                            final String nomTarea = nombreTareaActual;

                            TareaProyecto tareaProyecto = tareaProyectoRepository
                                .findByIdProyectoAndIdFaseAndIdDepartamentoAndTarea(proyectoId, idSubfase, idDepto, nomTarea)
                                .orElseGet(() -> {
                                    TareaProyecto tp = new TareaProyecto();
                                    tp.setIdProyecto(proyectoId);
                                    tp.setIdFase(idSubfase);
                                    tp.setIdDepartamento(idDepto);
                                    tp.setTarea(nomTarea);
                                    tp.setCompletada(false);
                                    return tareaProyectoRepository.save(tp);
                                });

                            DetalleEstimacion detalle = new DetalleEstimacion();
                            detalle.setIdExcel(idExcelGenerado);
                            detalle.setIdTareaProyecto(tareaProyecto.getIdTareaProyecto());
                            detalle.setTiempoMin(min != null ? min : 0.0);
                            detalle.setTiempoMax(max != null ? max : 0.0);
                            
                            listaParaGuardar.add(detalle);
                        }
                    }
                }
            }

            workbook.close();
            System.out.println("Total a guardar: " + listaParaGuardar.size());
            if (!listaParaGuardar.isEmpty()) {
                detalleEstimacionRepository.saveAll(listaParaGuardar);
            }
            return listaParaGuardar.size();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Limpia un texto eliminando espacios, pasando a minúsculas y removiendo tildes o caracteres especiales.
     * @param texto El texto original.
     * @return El texto completamente normalizado.
     */
    public String normalizarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }
        String limpio = texto.trim().toLowerCase();
        String normalizado = java.text.Normalizer.normalize(limpio, java.text.Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Identifica el ID de un departamento en la base de datos partiendo de su nombre normalizado.
     * @param nombre Nombre del departamento.
     * @return ID del departamento o -1 si no se localiza.
     */
    private int determinarDepartamento(String nombre) {
        String nombreLimpio = normalizarTexto(nombre);
        return departamentoRepository.findAll().stream()
                .filter(d -> { return normalizarTexto(d.getNombre()).equals(nombreLimpio); })
                .map(d -> { return d.getId(); })
                .findFirst()
                .orElse(-1);
    }

    /**
     * Extrae el valor de una celda numérica evaluando de forma segura fórmulas y parseando strings.
     * @param celda Celda de Apache POI.
     * @param evaluator Evaluador de fórmulas del libro de Excel.
     * @return Double con el valor de la celda o null.
     */
    private Double extraerNumeroDeCelda(Cell celda, FormulaEvaluator evaluator) {
        if (celda == null) { 
            return null; 
        }
        CellType type = celda.getCellType();
        if (type == CellType.FORMULA) {
            try {
                CellValue cv = evaluator.evaluate(celda);
                if (cv.getCellType() == CellType.NUMERIC) { 
                    return cv.getNumberValue(); 
                }
            } catch (Exception e) { 
                return null; 
            }
        }
        if (type == CellType.NUMERIC) { 
            return celda.getNumericCellValue(); 
        }
        if (type == CellType.STRING) {
            try { 
                return Double.parseDouble(celda.getStringCellValue().trim().replace(",", ".")); 
            } catch (Exception e) { 
                return null; 
            }
        }
        return null;
    }

    /**
     * Evalúa si se ha llegado al final de la matriz útil de estimaciones detectando el string "total".
     * @param fila Fila del libro Excel.
     * @return True si la fila marca el fin del procesamiento.
     */
    private boolean esFilaFinal(Row fila) {
        if (fila == null) { 
            return true; 
        }
        Cell cellA = fila.getCell(0);
        return cellA != null && normalizarTexto(cellA.toString()).contains("total");
    }

   /**
     * Recupera y enriquece los detalles de estimación de un Excel cruzando los datos numéricos con el pivote TareaProyecto.
     * @param idExcel ID del libro Excel.
     * @return Lista de DTOs mapeados con los nombres de fases y departamentos.
     */
    public List<DetalleEstimacionDTO> obtenerDetallesPorExcel(Integer idExcel) {
        List<DetalleEstimacion> detalles = detalleEstimacionRepository.findByIdExcel(idExcel);
        System.out.println("[LOG LECTURA] Tareas numéricas encontradas en detalle_estimacion: " + detalles.size() + " para el Excel ID: " + idExcel);

        List<Fase> todasLasFases = faseRepository.findAll();
        List<Departamento> todosLosDeptos = departamentoRepository.findAll();

        Excel excel = excelRepository.findById(idExcel).orElse(null);
        Long idProyecto = (excel != null) ? excel.getIdProyecto() : null;
        System.out.println("[LOG LECTURA] Proyecto ID asociado al Excel: " + idProyecto);
        
        List<TareaProyecto> todasTareasProyecto = (idProyecto != null) 
            ? tareaProyectoRepository.findAll().stream().filter(t -> { return t.getIdProyecto().equals(idProyecto); }).collect(Collectors.toList())
            : new ArrayList<>();
        System.out.println("[LOG LECTURA] Tareas totales encontradas en el pivote tarea_proyecto para este proyecto: " + todasTareasProyecto.size());
            
        Map<Long, TareaProyecto> mapaTareasProyecto = todasTareasProyecto.stream()
            .collect(Collectors.toMap(TareaProyecto::getIdTareaProyecto, t -> { return t; }, (a, b) -> { return a; }));

        return detalles.stream().map(entidad -> {
            DetalleEstimacionDTO dto = new DetalleEstimacionDTO();
            dto.setId(entidad.getId()); 
            dto.setIdExcel(entidad.getIdExcel());
            dto.setIdTareaProyecto(entidad.getIdTareaProyecto());
            dto.setTiempoMin(entidad.getTiempoMin());
            dto.setTiempoMax(entidad.getTiempoMax());

            TareaProyecto tp = mapaTareasProyecto.get(entidad.getIdTareaProyecto());
            if (tp != null) {
                dto.setIdDepartamento(tp.getIdDepartamento());
                dto.setIdSubFase(tp.getIdFase());
                dto.setTarea(tp.getTarea());
                dto.setCompletada(tp.getCompletada());

                String nombreDepto = todosLosDeptos.stream()
                        .filter(d -> { return d.getId() == tp.getIdDepartamento(); }) 
                        .map(Departamento::getNombre)
                        .findFirst()
                        .orElse("Desconocido");
                dto.setNombreDepartamento(nombreDepto);

                Fase subfase = todasLasFases.stream()
                        .filter(f -> { return f.getId().equals(tp.getIdFase()); })
                        .findFirst()
                        .orElse(null);

                if (subfase != null) {
                    dto.setNombreSubfase(subfase.getNombre());

                    if (subfase.getFasePadre() != null) {
                        String nombrePadre = todasLasFases.stream()
                                .filter(f -> { return f.getId().equals(subfase.getFasePadre()); })
                                .map(Fase::getNombre)
                                .findFirst()
                                .orElse("Desconocido");
                        dto.setNombreFase(nombrePadre);
                    } else {
                        dto.setNombreFase(subfase.getNombre());
                        dto.setNombreSubfase("-");
                    }
                } else {
                    dto.setNombreFase("Desconocido");
                    dto.setNombreSubfase("Desconocido");
                }
            } else {
                System.out.println("[ALERTA] No se encontró el pivote TareaProyecto para el id_tarea_proyecto: " + entidad.getIdTareaProyecto());
            }
            return dto;
        }).collect(Collectors.toList());
    }
    /**
     * Recupera las entidades de estimación puras (numéricas) asociadas a un Excel.
     * @param idExcel ID del Excel.
     * @return Lista de DetalleEstimacion.
     */
    public List<DetalleEstimacion> obtenerDetallesEntidadPorExcel(Integer idExcel) {
        return detalleEstimacionRepository.findByIdExcel(idExcel);
    }

    /**
     * Busca las estimaciones detalladas que coinciden con un proyecto, subfase y nombre de tarea específicos cruzando datos con el pivote.
     * @param idProyecto ID del proyecto.
     * @param idSubfase ID de la subfase.
     * @param nombreTarea Texto de la tarea.
     * @param idExcelElegido ID opcional del Excel del historial.
     * @return Lista de DTOs desglosados por departamento con sus respectivos tiempos estimados y reales.
     */
    public List<DetalleEstimacionDTO> obtenerDetallePorCriterios(Long idProyecto, Integer idSubfase, String nombreTarea, Integer idExcelElegido) {
        Integer idExcelBase = resolverIdExcelBase(idProyecto, idExcelElegido);
        if (idExcelBase == null) {
            return new ArrayList<>();
        }

        List<DetalleEstimacion> todasLasEstimaciones = detalleEstimacionRepository.findByIdExcel(idExcelBase);
        List<Departamento> todosLosDeptos = departamentoRepository.findAll();
        
        List<TareaProyecto> todasTareasProyecto = tareaProyectoRepository.findAll().stream()
                .filter(t -> { return t.getIdProyecto().equals(idProyecto); })
                .collect(Collectors.toList());
                
        Map<Long, TareaProyecto> mapaTareasProyecto = todasTareasProyecto.stream()
                .collect(Collectors.toMap(TareaProyecto::getIdTareaProyecto, t -> { return t; }, (a, b) -> { return a; }));

        return todasLasEstimaciones.stream()
                .filter(d -> {
                    TareaProyecto tp = mapaTareasProyecto.get(d.getIdTareaProyecto());
                    return tp != null && tp.getIdFase().equals(idSubfase) && tp.getTarea().equalsIgnoreCase(nombreTarea.trim());
                })
                .map(entidad -> {
                    TareaProyecto tp = mapaTareasProyecto.get(entidad.getIdTareaProyecto());
                    DetalleEstimacionDTO dto = new DetalleEstimacionDTO();
                    dto.setId(entidad.getId());
                    dto.setIdExcel(entidad.getIdExcel());
                    dto.setIdTareaProyecto(entidad.getIdTareaProyecto());
                    dto.setTiempoMin(entidad.getTiempoMin());
                    dto.setTiempoMax(entidad.getTiempoMax());

                    if (tp != null) {
                        dto.setIdDepartamento(tp.getIdDepartamento());
                        dto.setIdSubFase(tp.getIdFase());
                        dto.setTarea(tp.getTarea());
                        dto.setCompletada(tp.getCompletada());

                        String nombreDepto = todosLosDeptos.stream()
                            .filter(d -> { return d.getId() == tp.getIdDepartamento(); })
                            .map(Departamento::getNombre)
                            .findFirst()
                            .orElse("Desconocido");
                        dto.setNombreDepartamento(nombreDepto);
                    }

                    Double horasReales = imputacionClockifyRepository.sumarHorasPorTarea(entidad.getIdTareaProyecto());
                    dto.setTiempoReal(horasReales != null ? Math.round(horasReales * 10.0) / 10.0 : 0.0);

                    return dto;
                })
                .collect(Collectors.toList());
    }

  /**
     * Obtiene e integra los totales estimativos y reales de las tareas de una subfase del proyecto actual.
     * @param idProyecto ID del proyecto.
     * @param idSubfase ID de la subfase.
     * @param idExcelElegido ID opcional del Excel.
     * @return Lista de DTOs de las tareas agregadas de la subfase.
     */
    public List<TareaSubfaseDTO> obtenerTareasSubfase(long idProyecto, Integer idSubfase, Integer idExcelElegido){
        System.out.println("[LOG SUBFASE] Solicitando tareas de Subfase ID: " + idSubfase + " para Proyecto ID: " + idProyecto);
        Integer idExcelBase = resolverIdExcelBase(idProyecto, idExcelElegido);
        if (idExcelBase == null) {
            System.out.println("[LOG SUBFASE] No se localizó ningún Excel base activo.");
            return new ArrayList<>();
        }

        List<DetalleEstimacion> todasLasEstimaciones = detalleEstimacionRepository.findByIdExcel(idExcelBase);
        
        List<TareaProyecto> todasTareasProyecto = tareaProyectoRepository.findAll().stream()
                .filter(t -> { return t.getIdProyecto().equals(idProyecto); })
                .collect(Collectors.toList());
                
        Map<Long, TareaProyecto> mapaTareasProyecto = todasTareasProyecto.stream()
                .collect(Collectors.toMap(TareaProyecto::getIdTareaProyecto, t -> { return t; }, (a, b) -> { return a; }));

        Map<String, List<DetalleEstimacion>> tareasAgrupadas = todasLasEstimaciones.stream()
                .filter(d -> {
                    TareaProyecto tp = mapaTareasProyecto.get(d.getIdTareaProyecto());
                    return tp != null && tp.getIdFase().equals(idSubfase);
                })
                .collect(Collectors.groupingBy(d -> {
                    TareaProyecto tp = mapaTareasProyecto.get(d.getIdTareaProyecto());
                    return tp != null ? tp.getTarea() : "Desconocido";
                }));

        System.out.println("[LOG SUBFASE] Cantidad de tareas agrupadas listas para enviar al Front: " + tareasAgrupadas.size());

        List<TareaSubfaseDTO> resultado = new ArrayList<>();
        
        for (Map.Entry<String, List<DetalleEstimacion>> entry : tareasAgrupadas.entrySet()) {
            TareaSubfaseDTO dto = new TareaSubfaseDTO();
            dto.setNombreTarea(entry.getKey());
            dto.setIdTarea(entry.getValue().get(0).getId());

            double sumaMin = entry.getValue().stream().mapToDouble(DetalleEstimacion::getTiempoMin).sum();
            double sumaMax = entry.getValue().stream().mapToDouble(DetalleEstimacion::getTiempoMax).sum();

            double sumaReal = 0.0;
            for (DetalleEstimacion det : entry.getValue()) {
                Double horasReales = imputacionClockifyRepository.sumarHorasValidas(det.getId());
                if (horasReales != null) {
                    sumaReal += horasReales;
                }
            }

            dto.setTiempoTotalMin(sumaMin);
            dto.setTiempoTotalMax(sumaMax);
            dto.setTiempoTotalReal(Math.round(sumaReal * 10.0) / 10.0);
            
            resultado.add(dto);
        }

        if (idExcelElegido != null) {
            List<DetalleEstimacion> estimacionesElegidas = detalleEstimacionRepository.findByIdExcel(idExcelElegido);
            
            Map<String, List<DetalleEstimacion>> agrupadasElegidas = estimacionesElegidas.stream()
                    .filter(d -> {
                        TareaProyecto tp = mapaTareasProyecto.get(d.getIdTareaProyecto());
                        return tp != null && tp.getIdFase().equals(idSubfase);
                    })
                    .collect(Collectors.groupingBy(d -> {
                        TareaProyecto tp = mapaTareasProyecto.get(d.getIdTareaProyecto());
                        return tp != null ? tp.getTarea() : "Desconocido";
                    }));

            for (TareaSubfaseDTO dto : resultado) {
                if (agrupadasElegidas.containsKey(dto.getNombreTarea())) {
                    List<DetalleEstimacion> tareasElegidas = agrupadasElegidas.get(dto.getNombreTarea());
                    
                    double sumaMinElegido = tareasElegidas.stream().mapToDouble(DetalleEstimacion::getTiempoMin).sum();
                    double sumaMaxElegido = tareasElegidas.stream().mapToDouble(DetalleEstimacion::getTiempoMax).sum();
                    
                    dto.setTiempoTotalMinElegido(sumaMinElegido);
                    dto.setTiempoTotalMaxElegido(sumaMaxElegido);
                }
            }
        }

        return resultado;
    }
    /**
     * Calcula masivamente los resúmenes y medias de tiempos de todas las subfases indexándolos por el ID de subfase.
     * @param idProyecto ID del proyecto.
     * @return Map indexado por ID de subfase con sus resúmenes numéricos de tiempos reales y medias estimadas.
     */
    public Map<Integer, ResumenTiemposDTO> obtenerResumenTodasSubfases(Long idProyecto) {
        Map<Integer, ResumenTiemposDTO> resultado = new HashMap<>();
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        
        if (excel == null) {
            return resultado;
        }

        List<DetalleEstimacion> todasLasTareas = detalleEstimacionRepository.findByIdExcel(excel.getIdExcel());
        List<Object[]> sumasBD = imputacionClockifyRepository.sumarHorasValidasAgrupadasPorTarea(idProyecto);
        
        Map<Long, Double> mapaHorasReales = new HashMap<>();
        for (Object[] fila : sumasBD) {
            Long idDetalle = (Long) fila[0];
            Double suma = (Double) fila[1];
            mapaHorasReales.put(idDetalle, suma != null ? suma : 0.0);
        }

        List<TareaProyecto> todasTareasProyecto = tareaProyectoRepository.findAll().stream()
                .filter(t -> { return t.getIdProyecto().equals(idProyecto); })
                .collect(Collectors.toList());
                
        Map<Long, TareaProyecto> mapaTareasProyecto = todasTareasProyecto.stream()
                .collect(Collectors.toMap(TareaProyecto::getIdTareaProyecto, t -> { return t; }, (a, b) -> { return a; }));

        Map<Integer, List<DetalleEstimacion>> tareasPorSubfase = todasLasTareas.stream()
            .filter(d -> { return mapaTareasProyecto.containsKey(d.getIdTareaProyecto()); })
            .collect(Collectors.groupingBy(d -> {
                TareaProyecto tp = mapaTareasProyecto.get(d.getIdTareaProyecto());
                return tp != null ? tp.getIdFase() : -1;
            }));

        for (Map.Entry<Integer, List<DetalleEstimacion>> entry : tareasPorSubfase.entrySet()) {
            Integer idSubfase = entry.getKey();
            if (idSubfase == -1) {
                continue;
            }
            List<DetalleEstimacion> tareas = entry.getValue();

            double sumaRealTotal = 0.0;
            double sumaMinTotal = 0.0;
            double sumaMaxTotal = 0.0;

            for (DetalleEstimacion det : tareas) {
                Double realValido = mapaHorasReales.getOrDefault(det.getIdTareaProyecto(), 0.0);
                
                sumaRealTotal += realValido;
                sumaMinTotal += (det.getTiempoMin() != null ? det.getTiempoMin() : 0.0);
                sumaMaxTotal += (det.getTiempoMax() != null ? det.getTiempoMax() : 0.0);
            }

            double mediaEstimada = (sumaMinTotal + sumaMaxTotal) / 2.0;
            
            sumaRealTotal = Math.round(sumaRealTotal * 10.0) / 10.0;
            mediaEstimada = Math.round(mediaEstimada * 10.0) / 10.0;

            resultado.put(idSubfase, new ResumenTiemposDTO(sumaRealTotal, mediaEstimada));
        }

        return resultado;
    }

    /**
     * Alias histórico para obtener los desgloses específicos cruzando la información con el pivote TareaProyecto.
     * @param idProyecto ID del proyecto.
     * @param idSubfase ID de la subfase.
     * @param nombreTarea Texto descriptivo de la tarea.
     * @param idExcelElegido ID del documento Excel a forzar.
     * @return Lista de DTOs enriquecidos listos para renderizar.
     */
    public List<DetalleEstimacionDTO> obtenerDetallePorCriteriosHistorico(Long idProyecto, Integer idSubfase, String nombreTarea, Integer idExcelElegido) {
        return obtenerDetallePorCriterios(idProyecto, idSubfase, nombreTarea, idExcelElegido);
    }

    /**
     * Alias histórico para recuperar agregaciones de tareas por subfase vinculadas al pivote.
     * @param idProyecto ID del proyecto.
     * @param idSubfase ID de la subfase.
     * @param idExcelElegido ID opcional del Excel del historial.
     * @return Lista agregada de tareas de subfase.
     */
    public List<TareaSubfaseDTO> obtenerTareasSubfaseHistorico(long idProyecto, Integer idSubfase, Integer idExcelElegido) {
        return obtenerTareasSubfase(idProyecto, idSubfase, idExcelElegido);
    }

    /**
     * Alias histórico para la recopilación masiva de resúmenes de subfase en base al pivote.
     * @param idProyecto ID del proyecto.
     * @param idExcelElegido ID del documento Excel.
     * @return Map estructurado con los cálculos agregados.
     */
    public Map<Integer, ResumenTiemposDTO> obtenerResumenTodasSubfasesHistorico(Long idProyecto, Integer idExcelElegido) {
        return obtenerResumenTodasSubfases(idProyecto);
    }

    /**
     * Resuelve de forma segura qué ID de Excel se debe utilizar como base para las consultas analíticas.
     * @param idProyecto ID del proyecto local.
     * @param idExcelElegido ID explícito enviado, o null si se requiere usar el vigente.
     * @return El Integer correspondiente al ID del documento definitivo o null.
     */
    private Integer resolverIdExcelBase(Long idProyecto, Integer idExcelElegido) {
        if (idExcelElegido != null) {
            return idExcelElegido;
        }
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        return excel != null ? excel.getIdExcel() : null;
    }

    /**
     * Obtiene el mapa completo de resolución de nombres de fases de la base de datos.
     * @return Un mapa indexando IDs de Fases con sus nombres textuales.
     */
    private Map<Integer, String> obtenerMapaNombresFase() {
        return faseRepository.findAll().stream()
                .collect(Collectors.toMap(Fase::getId, Fase::getNombre, (a, b) -> { return a; }));
    }

    /**
     * Calcula los totales completos de un proyecto calculando la suma de tiempos reales válidos globales 
     * y la media matemática total del presupuesto del Excel establecido como vigente.
     * @param idProyecto ID del proyecto a resumir.
     * @return DTO ResumenTiemposDTO con los resultados matemáticos listos.
     */
    public ResumenTiemposDTO obtenerResumenProyecto(Long idProyecto) {
        Double realTotal = imputacionClockifyRepository.sumarHorasTotalesProyecto(idProyecto);
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        double mediaEstimada = 0.0;
        
        if (excel != null) {
            List<DetalleEstimacion> todasLasTareas = detalleEstimacionRepository.findByIdExcel(excel.getIdExcel());
            
            double sumaMin = todasLasTareas.stream()
                    .mapToDouble(t -> { return t.getTiempoMin() != null ? t.getTiempoMin() : 0.0; })
                    .sum();
            double sumaMax = todasLasTareas.stream()
                    .mapToDouble(t -> { return t.getTiempoMax() != null ? t.getTiempoMax() : 0.0; })
                    .sum();
            
            mediaEstimada = (sumaMin + sumaMax) / 2.0;
        }

        double realRedondeado = Math.round((realTotal != null ? realTotal : 0.0) * 10.0) / 10.0;
        double mediaRedondeada = Math.round(mediaEstimada * 10.0) / 10.0;

        return new ResumenTiemposDTO(realRedondeado, mediaRedondeada);
    }

    /**
     * Procesa masivamente los resúmenes de una colección de proyectos en bloque.
     * @param idsProyectos Lista de identificadores de proyectos.
     * @return Map asociando el ID del proyecto con su DTO de resumen correspondiente.
     */
    public Map<Long, ResumenTiemposDTO> obtenerResumenVariosProyectos(List<Long> idsProyectos) {
        Map<Long, ResumenTiemposDTO> resultados = new HashMap<>();
        for (Long idProy : idsProyectos) {
            resultados.put(idProy, obtenerResumenProyecto(idProy));
        }
        return resultados;
    }

    /**
     * Modifica los valores matemáticos de un presupuesto en BD y actualiza de forma segura el pivote textual TareaProyecto asociado.
     * @param id ID único de la fila del presupuesto de estimación.
     * @param detalleDTO DTO con la nueva información descriptiva o presupuestaria.
     * @return El DetalleEstimacion actualizado y persistido en el sistema.
     */
    @Auditable(
        accion = "ACTUALIZAR_ESTIMACION", 
        tabla = "detalle_estimacion", 
        entidad = DetalleEstimacion.class,
        descripcion = "Se actualizaron los datos numéricos de la estimación."
    )
    public DetalleEstimacion actualizarDetalle(Long id, DetalleEstimacionDTO detalleDTO) {
        DetalleEstimacion detalle = detalleEstimacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró la tarea con ID: " + id));

        if (detalleDTO.getTiempoMax() != null) { 
            detalle.setTiempoMax(detalleDTO.getTiempoMax()); 
        }
        if (detalleDTO.getTiempoMin() != null) { 
            detalle.setTiempoMin(detalleDTO.getTiempoMin()); 
        }

        TareaProyecto tp = tareaProyectoRepository.findById(detalle.getIdTareaProyecto())
            .orElseThrow(() -> new RuntimeException("No se localizó la tarea global pivote asociada."));

        if (detalleDTO.getTarea() != null) { 
            tp.setTarea(detalleDTO.getTarea().trim()); 
        }
        if (detalleDTO.getIdDepartamento() != null) { 
            tp.setIdDepartamento(detalleDTO.getIdDepartamento()); 
        }
        if (detalleDTO.getIdSubFase() != null) { 
            tp.setIdFase(detalleDTO.getIdSubFase()); 
        }
        
        tareaProyectoRepository.save(tp);
        return detalleEstimacionRepository.save(detalle);
    }

    /**
     * Da de alta un nuevo presupuesto de estimación manual aplicando el patrón Find-or-Create en la TareaProyecto global.
     * @param dto DTO que contiene la identidad conceptual de la tarea y sus nuevos tiempos estimados.
     * @return La nueva entidad DetalleEstimacion registrada con éxito.
     */
    @Auditable(
        accion = "CREAR_ESTIMACION", 
        tabla = "detalle_estimacion", 
        entidad = DetalleEstimacion.class,
        descripcion = "Se creó manualmente una estimación de tarea."
    )
    public DetalleEstimacion crearTarea(DetalleEstimacionDTO dto) {
        if (dto.getTarea() == null || dto.getTarea().trim().isEmpty()) {
            throw new RuntimeException("El nombre de la tarea es obligatorio.");
        }
        if (dto.getIdExcel() == null || dto.getIdSubFase() == null || dto.getIdDepartamento() == null) {
            throw new RuntimeException("Faltan datos obligatorios (excel, fase o departamento).");
        }
        if (dto.getTiempoMin() == null || dto.getTiempoMax() == null) {
            throw new RuntimeException("Los tiempos mínimo y máximo son obligatorios.");
        }
        if (dto.getTiempoMin() < 0 || dto.getTiempoMax() < 0) {
            throw new RuntimeException("Los tiempos no pueden ser negativos.");
        }
        if (dto.getTiempoMin() > dto.getTiempoMax()) {
            throw new RuntimeException("El tiempo mínimo no puede ser mayor que el máximo.");
        }

        Excel excel = excelRepository.findById(dto.getIdExcel())
            .orElseThrow(() -> new RuntimeException("El documento Excel base no existe."));
        Long idProyecto = excel.getIdProyecto();

        TareaProyecto tareaProyecto = tareaProyectoRepository
            .findByIdProyectoAndIdFaseAndIdDepartamentoAndTarea(idProyecto, dto.getIdSubFase(), dto.getIdDepartamento(), dto.getTarea().trim())
            .orElseGet(() -> {
                TareaProyecto tp = new TareaProyecto();
                tp.setIdProyecto(idProyecto);
                tp.setIdFase(dto.getIdSubFase());
                tp.setIdDepartamento(dto.getIdDepartamento());
                tp.setTarea(dto.getTarea().trim());
                tp.setCompletada(false);
                return tareaProyectoRepository.save(tp);
            });

        DetalleEstimacion nueva = new DetalleEstimacion();
        nueva.setIdExcel(dto.getIdExcel());
        nueva.setIdTareaProyecto(tareaProyecto.getIdTareaProyecto());
        nueva.setTiempoMin(dto.getTiempoMin());
        nueva.setTiempoMax(dto.getTiempoMax());

        return detalleEstimacionRepository.save(nueva);
    }

    /**
     * Elimina de forma permanente un presupuesto de estimación específico de la base de datos.
     * @param id ID único de la estimación numérico a remover.
     * @return La entidad DetalleEstimacion eliminada.
     */
    @Auditable(
        accion = "BORRAR_ESTIMACION", 
        tabla = "detalle_estimacion", 
        entidad = DetalleEstimacion.class,
        descripcion = "Se eliminó la estimación asociada a la tarea con ID '#{#resultado.idTareaProyecto}'"
    )
    public DetalleEstimacion eliminarTarea(Long id) {
        DetalleEstimacion detalle = detalleEstimacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró la tarea con ID: " + id));

        detalleEstimacionRepository.delete(detalle);
        return detalle;
    }
}