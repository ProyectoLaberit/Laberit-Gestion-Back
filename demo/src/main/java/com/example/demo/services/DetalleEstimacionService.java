package com.example.demo.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Autowired
    private com.example.demo.repository.ImputacionClockifyRepository imputacionClockifyRepository;

    /**
     * Procesa un archivo Excel físico, extrae las estimaciones y las guarda en BD.
     * * @param archivo El archivo MultipartFile subido desde el cliente.
     * @param proyectoId ID del proyecto al que pertenece la estimación.
     * @param usuarioId ID del usuario que sube el archivo.
     * @return El número total de tareas válidas insertadas en la base de datos.
     */
    @Auditable(
        accion = "IMPORTAR_EXCEL", 
        tabla = "excel", 
        entidad = Excel.class,
        descripcion = "Se importó un nuevo documento Excel para el proyecto con ID: #{#proyectoId}"
    )
    public int procesarExcel(MultipartFile archivo, long proyectoId, Integer usuarioId) throws Exception {
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

        List<RangoDepartamento> mapaColumnas = Arrays.asList(
            new RangoDepartamento(3, 4, "Comercial"), new RangoDepartamento(5, 6, "Direccion"),
            new RangoDepartamento(7, 8, "back"), new RangoDepartamento(9, 10, "front"),
            new RangoDepartamento(11, 12, "soporte"), new RangoDepartamento(13, 14, "mk"),
            new RangoDepartamento(15, 16, "ux"), new RangoDepartamento(17, 18, "ui"),
            new RangoDepartamento(19, 20, "wp-maq")
        );

        for (RangoDepartamento rango : mapaColumnas) {
            rango.setIdBD(determinarDepartamento(rango.getNombreExcel()));
        }

        Sheet hoja = workbook.getSheetAt(0);
        
        Integer idFasePadreActual = null;
        Integer idSubfaseActual = null;
        String nombreTareaActual = null; 

        for (Row fila : hoja) {
            if (fila.getRowNum() < 4) { continue; }
            if (esFilaFinal(fila)) { break; }

            String valFase = formatter.formatCellValue(fila.getCell(0));
            String faseLimpia = normalizarTexto(valFase);
            
            if (!faseLimpia.isEmpty()) {
                idFasePadreActual = todasLasFasesBD.stream()
                    .filter(f -> f.getFasePadre() == null && normalizarTexto(f.getNombre()).equals(faseLimpia))
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
                    .filter(f -> f.getFasePadre() != null && f.getFasePadre().equals(idPadre) && normalizarTexto(f.getNombre()).equals(subfaseLimpia))
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

            if (idSubfaseActual != null && nombreTareaActual != null) {
                for (RangoDepartamento depto : mapaColumnas) {
                    if (depto.getIdBD() == null || depto.getIdBD() == -1) {
                        continue;
                    }

                    Double min = extraerNumeroDeCelda(fila.getCell(depto.getColMin()), evaluator);
                    Double max = extraerNumeroDeCelda(fila.getCell(depto.getColMax()), evaluator);

                    if ((min != null && min > 0) || (max != null && max > 0)) {
                        DetalleEstimacion detalle = new DetalleEstimacion();
                        detalle.setIdExcel(idExcelGenerado);
                        detalle.setIdDepartamento(depto.getIdBD());
                        detalle.setIdFase(idSubfaseActual);
                        detalle.setTarea(nombreTareaActual);
                        
                        detalle.setTiempoMin(min != null ? min : 0.0);
                        detalle.setTiempoMax(max != null ? max : 0.0);
                        
                        listaParaGuardar.add(detalle);
                    }
                }
            }
        }

        workbook.close();
        if (!listaParaGuardar.isEmpty()) {
            detalleEstimacionRepository.saveAll(listaParaGuardar);
        }
        return listaParaGuardar.size();
    }

    // ==========================================================
    // MÉTODOS AUXILIARES Y NORMALIZACIÓN
    // ==========================================================

    /**
     * Limpia un texto: quita espacios, pasa a minúsculas y elimina tildes.
     * Vital para comparar cadenas del Excel (ej. "Análisis") con BD ("analisis").
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
     * Busca el ID de un departamento en la BD basándose en su nombre limpio.
     */
    private int determinarDepartamento(String nombre) {
        String nombreLimpio = normalizarTexto(nombre);
        return departamentoRepository.findAll().stream()
                .filter(d -> normalizarTexto(d.getNombre()).equals(nombreLimpio))
                .map(d -> d.getId())
                .findFirst()
                .orElse(-1);
    }

    /**
     * Extrae el valor numérico de una celda con extrema seguridad.
     * Evalúa fórmulas dinámicas, recoge números crudos y parsea Strings (sustituyendo comas por puntos).
     */
    private Double extraerNumeroDeCelda(Cell celda, FormulaEvaluator evaluator) {
        if (celda == null) { return null; }
        CellType type = celda.getCellType();
        if (type == CellType.FORMULA) {
            try {
                CellValue cv = evaluator.evaluate(celda);
                if (cv.getCellType() == CellType.NUMERIC) { return cv.getNumberValue(); }
            } catch (Exception e) { return null; }
        }
        if (type == CellType.NUMERIC) { return celda.getNumericCellValue(); }
        if (type == CellType.STRING) {
            try { return Double.parseDouble(celda.getStringCellValue().trim().replace(",", ".")); } catch (Exception e) { return null; }
        }
        return null;
    }

    /**
     * Detiene el procesado si encuentra la palabra "total" en la primera columna, 
     * evitando leer filas basura al final del Excel.
     */
    private boolean esFilaFinal(Row fila) {
        if (fila == null) { return true; }
        Cell cellA = fila.getCell(0);
        return cellA != null && normalizarTexto(cellA.toString()).contains("total");
    }

    // ==========================================================
    // MÉTODOS DE CONSULTA Y EXPORTACIÓN
    // ==========================================================

    /**
     * Obtiene el listado completo de estimaciones para mostrar en una tabla visual del Frontend.
     * Carga los nombres bonitos (String) de Departamentos y Fases para facilitar el pintado.
     * * @param idExcel ID del Excel vigente a consultar.
     * @return Lista de DTOs enriquecidos con datos de texto.
     */
    public List<DetalleEstimacionDTO> obtenerDetallesPorExcel(Integer idExcel) {
        List<DetalleEstimacion> detalles = detalleEstimacionRepository.findByIdExcel(idExcel);
        List<Fase> todasLasFases = faseRepository.findAll();
        List<Departamento> todosLosDeptos = departamentoRepository.findAll();

        return detalles.stream().map(entidad -> {
            DetalleEstimacionDTO dto = new DetalleEstimacionDTO();
            dto.setId(entidad.getId()); 
            dto.setIdExcel(entidad.getIdExcel());
            dto.setIdDepartamento(entidad.getIdDepartamento());
            dto.setIdSubFase(entidad.getIdFase());
            dto.setTarea(entidad.getTarea());
            dto.setTiempoMin(entidad.getTiempoMin());
            dto.setTiempoMax(entidad.getTiempoMax());

            String nombreDepto = todosLosDeptos.stream()
                    .filter(d -> d.getId() == entidad.getIdDepartamento()) 
                    .map(d -> d.getNombre())
                    .findFirst()
                    .orElse("Desconocido");
            dto.setNombreDepartamento(nombreDepto);

            Fase subfase = todasLasFases.stream()
                    .filter(f -> f.getId().equals(entidad.getIdFase()))
                    .findFirst()
                    .orElse(null);

            if (subfase != null) {
                dto.setNombreSubfase(subfase.getNombre());

                if (subfase.getFasePadre() != null) {
                    String nombrePadre = todasLasFases.stream()
                            .filter(f -> f.getId().equals(subfase.getFasePadre()))
                            .map(f -> f.getNombre())
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

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Obtiene los registros puros de la BD (sin transformar a DTO).
     */
    public List<DetalleEstimacion> obtenerDetallesEntidadPorExcel(Integer idExcel) {
        return detalleEstimacionRepository.findByIdExcel(idExcel);
    }

    /**
     * Consulta súper ligera para verificar tiempos u horas de una tarea en concreto.
     * No carga los nombres de diccionarios (Departamentos/Fases) por rendimiento.
     * * @param idProyecto Para buscar su excel vigente.
     * @param idSubfase Fase en la que se encuentra la tarea.
     * @param nombreTarea Nombre exacto (o casi exacto) de la tarea a buscar.
     * @return DTO ligero con la información matemática y de IDs.
     */
    public List<DetalleEstimacionDTO> obtenerDetallePorCriterios(Long idProyecto, Integer idSubfase, String nombreTarea, Integer idExcelElegido) {
        Integer idExcelBase = resolverIdExcelBase(idProyecto, idExcelElegido);
        if (idExcelBase == null) {
            return new java.util.ArrayList<>();
        }

        List<DetalleEstimacion> todasLasEstimaciones = detalleEstimacionRepository.findByIdExcel(idExcelBase);
        List<com.example.demo.entity.Departamento> todosLosDeptos = departamentoRepository.findAll();

        // 3. Filtrar todas las filas que coincidan con la subfase y el nombre de la tarea
        List<DetalleEstimacionDTO> listaPrincipal = todasLasEstimaciones.stream()
                .filter(d -> d.getIdFase() != null && d.getIdFase().equals(idSubfase))
                .filter(d -> d.getTarea() != null && d.getTarea().equalsIgnoreCase(nombreTarea.trim()))
                .map(entidad -> {
                    DetalleEstimacionDTO dto = new DetalleEstimacionDTO();
                    dto.setId(entidad.getId());
                    dto.setIdExcel(entidad.getIdExcel());
                    dto.setIdDepartamento(entidad.getIdDepartamento());
                    dto.setIdSubFase(entidad.getIdFase());
                    dto.setTarea(entidad.getTarea());
                    dto.setTiempoMin(entidad.getTiempoMin());
                    dto.setTiempoMax(entidad.getTiempoMax());

                    // 1. Añadimos el número de GitLab directamente de la entidad
                    dto.setNumeroGitlab(entidad.getNumeroGitlab());

                    // 2. Calculamos el tiempo real desde Clockify (redondeado a 1 decimal para evitar fallos visuales)
                    Double horasReales = imputacionClockifyRepository.sumarHorasPorDetalle(entidad.getId());
                    dto.setTiempoReal(horasReales != null ? Math.round(horasReales * 10.0) / 10.0 : 0.0);

                    // 3. Buscamos el nombre del departamento de forma segura (tu código original)
                    String nombreDepto = todosLosDeptos.stream()
                        .filter(d -> d.getId() == entidad.getIdDepartamento())
                        .map(d -> d.getNombre())
                        .findFirst()
                        .orElse("Desconocido");
                    
                    dto.setNombreDepartamento(nombreDepto);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());


        return listaPrincipal;

    }

    public List<TareaSubfaseDTO> obtenerTareasSubfase(long idProyecto, Integer idSubfase, Integer idExcelElegido){

       // 1. Con el id del proyecto buscar su excel vigente
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        if (excel == null) {
            return new ArrayList<>(); // Si no hay excel, devolvemos lista vacía
        }

        // 2. Con el excel vigente ir a la tabla de estimaciones y buscar las tareas asociadas
        List<DetalleEstimacion> todasLasEstimaciones = detalleEstimacionRepository.findByIdExcel(excel.getIdExcel());

        // 3. Filtrar por la subfase que queremos y agruparlas por su nombre
        Map<String, List<DetalleEstimacion>> tareasAgrupadas = todasLasEstimaciones.stream()
                .filter(d -> d.getIdFase() != null && d.getIdFase().equals(idSubfase))
                .filter(d -> d.getTarea() != null)
                .collect(Collectors.groupingBy(DetalleEstimacion::getTarea));

        // 4. Sumar los tiempos minimos y maximos para devolver solamente el nombre y esos tiempos
        List<TareaSubfaseDTO> resultado = new ArrayList<>();
        
        for (Map.Entry<String, List<DetalleEstimacion>> entry : tareasAgrupadas.entrySet()) {
            TareaSubfaseDTO dto = new TareaSubfaseDTO();
            dto.setNombreTarea(entry.getKey());

            dto.setIdTarea(entry.getValue().get(0).getId());

            double sumaMin = entry.getValue().stream().mapToDouble(DetalleEstimacion::getTiempoMin).sum();
            double sumaMax = entry.getValue().stream().mapToDouble(DetalleEstimacion::getTiempoMax).sum();

            // -- SUMAR TIEMPO REAL DE TODOS LOS DEPARTAMENTOS DE ESTA TAREA --
            double sumaReal = 0.0;
            for (DetalleEstimacion det : entry.getValue()) {
                Double horasReales = imputacionClockifyRepository.sumarHorasPorDetalle(det.getId());
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
            // Traemos las tareas del Excel que queremos comparar
            List<DetalleEstimacion> estimacionesElegidas = detalleEstimacionRepository.findByIdExcel(idExcelElegido);
            
            // Las agrupamos por tarea
            Map<String, List<DetalleEstimacion>> agrupadasElegidas = estimacionesElegidas.stream()
                    .filter(d -> d.getIdFase() != null && d.getIdFase().equals(idSubfase))
                    .filter(d -> d.getTarea() != null)
                    .collect(Collectors.groupingBy(DetalleEstimacion::getTarea));

            // Recorremos el resultado del excel en el que nos encontramos
            for (TareaSubfaseDTO dto : resultado) {
                // Si la tarea existe en el Excel elegido, sumamos sus tiempos
                if (agrupadasElegidas.containsKey(dto.getNombreTarea())) {
                    List<DetalleEstimacion> tareasElegidas = agrupadasElegidas.get(dto.getNombreTarea());
                    
                    double sumaMinElegido = tareasElegidas.stream().mapToDouble(DetalleEstimacion::getTiempoMin).sum();
                    double sumaMaxElegido = tareasElegidas.stream().mapToDouble(DetalleEstimacion::getTiempoMax).sum();
                    
                    // Guardamos los totales
                    dto.setTiempoTotalMinElegido(sumaMinElegido);
                    dto.setTiempoTotalMaxElegido(sumaMaxElegido);
                }
            }
        }

        return resultado;
    }

    /**
     * Calcula los totales de TODAS las subfases de un proyecto de una sola vez.
     */
    public Map<Integer, ResumenTiemposDTO> obtenerResumenTodasSubfases(Long idProyecto) {
        Map<Integer, ResumenTiemposDTO> resultado = new java.util.HashMap<>();
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        
        if (excel == null) {
            return resultado;
        }

        // Traemos todas las tareas del Excel
        List<DetalleEstimacion> todasLasTareas = detalleEstimacionRepository.findByIdExcel(excel.getIdExcel());
        
        // Traemos TODAS las sumas de horas del proyecto de golpe
        List<Object[]> sumasBD = imputacionClockifyRepository.sumarHorasValidasAgrupadasPorDetalle(idProyecto);
        
        // Convertimos el resultado en un diccionario rápido en memoria { idTarea : sumaHoras }
        Map<Long, Double> mapaHorasReales = new java.util.HashMap<>();
        for (Object[] fila : sumasBD) {
            Long idDetalle = (Long) fila[0];
            Double suma = (Double) fila[1];
            mapaHorasReales.put(idDetalle, suma != null ? suma : 0.0);
        }

        // Agrupamos las tareas por Subfase
        Map<Integer, List<DetalleEstimacion>> tareasPorSubfase = todasLasTareas.stream()
            .filter(d -> d.getIdFase() != null)
            .collect(Collectors.groupingBy(DetalleEstimacion::getIdFase));

        // Calculamos los totales
        for (Map.Entry<Integer, List<DetalleEstimacion>> entry : tareasPorSubfase.entrySet()) {
            Integer idSubfase = entry.getKey();
            List<DetalleEstimacion> tareas = entry.getValue();

            double sumaRealTotal = 0.0;
            double sumaMinTotal = 0.0;
            double sumaMaxTotal = 0.0;

            for (DetalleEstimacion det : tareas) {
                // Buscamos la suma en nuestro diccionario de memoria al instante
                Double realValido = mapaHorasReales.getOrDefault(det.getId(), 0.0);
                
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

    public List<DetalleEstimacionDTO> obtenerDetallePorCriteriosHistorico(
            Long idProyecto,
            Integer idSubfase,
            String nombreTarea,
            Integer idExcelElegido) {
        Integer idExcelBase = resolverIdExcelBase(idProyecto, idExcelElegido);
        if (idExcelBase == null) {
            return new ArrayList<>();
        }

        List<DetalleEstimacion> todasLasEstimaciones = detalleEstimacionRepository.findByIdExcel(idExcelBase);
        List<Departamento> todosLosDeptos = departamentoRepository.findAll();

        return todasLasEstimaciones.stream()
                .filter(d -> d.getIdFase() != null && d.getIdFase().equals(idSubfase))
                .filter(d -> d.getTarea() != null && d.getTarea().equalsIgnoreCase(nombreTarea.trim()))
                .map(entidad -> {
                    DetalleEstimacionDTO dto = new DetalleEstimacionDTO();
                    dto.setId(entidad.getId());
                    dto.setIdExcel(entidad.getIdExcel());
                    dto.setIdDepartamento(entidad.getIdDepartamento());
                    dto.setIdSubFase(entidad.getIdFase());
                    dto.setTarea(entidad.getTarea());
                    dto.setTiempoMin(entidad.getTiempoMin());
                    dto.setTiempoMax(entidad.getTiempoMax());
                    dto.setNumeroGitlab(entidad.getNumeroGitlab());

                    Double horasReales = imputacionClockifyRepository.sumarHorasPorDetalle(entidad.getId());
                    dto.setTiempoReal(horasReales != null ? Math.round(horasReales * 10.0) / 10.0 : 0.0);

                    String nombreDepto = todosLosDeptos.stream()
                            .filter(d -> d.getId() == entidad.getIdDepartamento())
                            .map(Departamento::getNombre)
                            .findFirst()
                            .orElse("Desconocido");

                    dto.setNombreDepartamento(nombreDepto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<TareaSubfaseDTO> obtenerTareasSubfaseHistorico(long idProyecto, Integer idSubfase, Integer idExcelElegido) {
        Integer idExcelBase = resolverIdExcelBase(idProyecto, idExcelElegido);
        if (idExcelBase == null) {
            return new ArrayList<>();
        }

        List<DetalleEstimacion> todasLasEstimaciones = detalleEstimacionRepository.findByIdExcel(idExcelBase);

        Map<String, List<DetalleEstimacion>> tareasAgrupadas = todasLasEstimaciones.stream()
                .filter(d -> d.getIdFase() != null && d.getIdFase().equals(idSubfase))
                .filter(d -> d.getTarea() != null)
                .collect(Collectors.groupingBy(DetalleEstimacion::getTarea));

        List<TareaSubfaseDTO> resultado = new ArrayList<>();

        for (Map.Entry<String, List<DetalleEstimacion>> entry : tareasAgrupadas.entrySet()) {
            TareaSubfaseDTO dto = new TareaSubfaseDTO();
            dto.setNombreTarea(entry.getKey());
            dto.setIdTarea(entry.getValue().get(0).getId());

            double sumaMin = entry.getValue().stream().mapToDouble(DetalleEstimacion::getTiempoMin).sum();
            double sumaMax = entry.getValue().stream().mapToDouble(DetalleEstimacion::getTiempoMax).sum();

            double sumaReal = 0.0;
            for (DetalleEstimacion det : entry.getValue()) {
                Double horasReales = imputacionClockifyRepository.sumarHorasPorDetalle(det.getId());
                if (horasReales != null) {
                    sumaReal += horasReales;
                }
            }

            dto.setTiempoTotalMin(sumaMin);
            dto.setTiempoTotalMax(sumaMax);
            dto.setTiempoTotalReal(Math.round(sumaReal * 10.0) / 10.0);
            resultado.add(dto);
        }

        return resultado;
    }

    public Map<Integer, ResumenTiemposDTO> obtenerResumenTodasSubfasesHistorico(Long idProyecto, Integer idExcelElegido) {
        Map<Integer, ResumenTiemposDTO> resultado = new java.util.HashMap<>();
        Integer idExcelBase = resolverIdExcelBase(idProyecto, idExcelElegido);

        if (idExcelBase == null) {
            return resultado;
        }

        List<DetalleEstimacion> todasLasTareas = detalleEstimacionRepository.findByIdExcel(idExcelBase);
        List<Object[]> sumasBD = imputacionClockifyRepository.sumarHorasValidasAgrupadasPorDetalle(idProyecto);

        Map<Long, Double> mapaHorasReales = new java.util.HashMap<>();
        for (Object[] fila : sumasBD) {
            Long idDetalle = (Long) fila[0];
            Double suma = (Double) fila[1];
            mapaHorasReales.put(idDetalle, suma != null ? suma : 0.0);
        }

        Map<Integer, List<DetalleEstimacion>> tareasPorSubfase = todasLasTareas.stream()
                .filter(d -> d.getIdFase() != null)
                .collect(Collectors.groupingBy(DetalleEstimacion::getIdFase));

        for (Map.Entry<Integer, List<DetalleEstimacion>> entry : tareasPorSubfase.entrySet()) {
            Integer idSubfase = entry.getKey();
            List<DetalleEstimacion> tareas = entry.getValue();

            double sumaRealTotal = 0.0;
            double sumaMinTotal = 0.0;
            double sumaMaxTotal = 0.0;

            for (DetalleEstimacion det : tareas) {
                Double realValido = mapaHorasReales.getOrDefault(det.getId(), 0.0);
                sumaRealTotal += realValido;
                sumaMinTotal += det.getTiempoMin() != null ? det.getTiempoMin() : 0.0;
                sumaMaxTotal += det.getTiempoMax() != null ? det.getTiempoMax() : 0.0;
            }

            double mediaEstimada = (sumaMinTotal + sumaMaxTotal) / 2.0;
            sumaRealTotal = Math.round(sumaRealTotal * 10.0) / 10.0;
            mediaEstimada = Math.round(mediaEstimada * 10.0) / 10.0;

            resultado.put(idSubfase, new ResumenTiemposDTO(sumaRealTotal, mediaEstimada));
        }

        return resultado;
    }

    private Integer resolverIdExcelBase(Long idProyecto, Integer idExcelElegido) {
        if (idExcelElegido != null) {
            return idExcelElegido;
        }

        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        return excel != null ? excel.getIdExcel() : null;
    }

    /**
     * Calcula los totales de un proyecto completo:
     * - La suma de todo el tiempo real validado del proyecto.
     * - La media entre la suma total de mínimos y máximos de su Excel vigente.
     */
    public ResumenTiemposDTO obtenerResumenProyecto(Long idProyecto) {
        // Tiempo real total
        Double realTotal = imputacionClockifyRepository.sumarHorasTotalesProyecto(idProyecto);
        
        // Tiempo estimado medio del Excel vigente
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        double mediaEstimada = 0.0;
        
        if (excel != null) {
            // Traemos todas las tareas de ese Excel
            List<DetalleEstimacion> todasLasTareas = detalleEstimacionRepository.findByIdExcel(excel.getIdExcel());
            
            // Sumamos todos los mínimos y todos los máximos del proyecto
            double sumaMin = todasLasTareas.stream()
                    .mapToDouble(t -> t.getTiempoMin() != null ? t.getTiempoMin() : 0.0)
                    .sum();
            double sumaMax = todasLasTareas.stream()
                    .mapToDouble(t -> t.getTiempoMax() != null ? t.getTiempoMax() : 0.0)
                    .sum();
            
            // Calculamos la media de la estimación total
            mediaEstimada = (sumaMin + sumaMax) / 2.0;
        }

        // Redondeo
        double realRedondeado = Math.round((realTotal != null ? realTotal : 0.0) * 10.0) / 10.0;
        double mediaRedondeada = Math.round(mediaEstimada * 10.0) / 10.0;

        return new ResumenTiemposDTO(realRedondeado, mediaRedondeada);
    }

    /**
     * Obtiene el resumen de tiempos para una lista de proyectos.
     * Devuelve un Map donde la clave es el ID del proyecto y el valor su resumen.
     */
    public Map<Long, ResumenTiemposDTO> obtenerResumenVariosProyectos(List<Long> idsProyectos) {
        Map<Long, ResumenTiemposDTO> resultados = new java.util.HashMap<>();
        
        for (Long idProy : idsProyectos) {
            // Aprovechamos el método para un solo proyecto
            resultados.put(idProy, obtenerResumenProyecto(idProy));
        }
        return resultados;
    }

    // ==========================================================
    // MÉTODOS DE CREACIÓN Y ACTUALIZACIÓN (Lógica de Negocio)
    // ==========================================================

    /**
     * Actualiza los valores de una tarea existente.
     */
   @Auditable(
        accion = "ACTUALIZAR_ESTIMACION", 
        tabla = "detalle_estimacion", 
        entidad = DetalleEstimacion.class,
        descripcion = "Se actualizaron los datos de la estimación '#{#resultado.tarea}'"
    )
    public DetalleEstimacion actualizarDetalle(Long id, DetalleEstimacionDTO detalleDTO) {
        DetalleEstimacion detalle = detalleEstimacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró la tarea con ID: " + id));

        // Solo actualizamos si el valor viene en el DTO
        if (detalleDTO.getTarea() != null) detalle.setTarea(detalleDTO.getTarea());
        if (detalleDTO.getIdDepartamento() != null) detalle.setIdDepartamento(detalleDTO.getIdDepartamento());
        if (detalleDTO.getIdSubFase() != null) detalle.setIdFase(detalleDTO.getIdSubFase());
        if (detalleDTO.getTiempoMax() != null) detalle.setTiempoMax(detalleDTO.getTiempoMax());
        if (detalleDTO.getTiempoMin() != null) detalle.setTiempoMin(detalleDTO.getTiempoMin());

        return detalleEstimacionRepository.save(detalle);
    }

    /**
     * Crea una nueva tarea de estimación manualmente.
     */
    @Auditable(
        accion = "CREAR_ESTIMACION", 
        tabla = "detalle_estimacion", 
        entidad = DetalleEstimacion.class,
        descripcion = "Se creó manualmente la estimación para la tarea '#{#dto.tarea}'"
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

        DetalleEstimacion nueva = new DetalleEstimacion();
        nueva.setIdExcel(dto.getIdExcel());
        nueva.setIdFase(dto.getIdSubFase());
        nueva.setIdDepartamento(dto.getIdDepartamento());
        nueva.setTarea(dto.getTarea().trim());
        nueva.setTiempoMin(dto.getTiempoMin());
        nueva.setTiempoMax(dto.getTiempoMax());

        return detalleEstimacionRepository.save(nueva);
    }

    /**
     * Elimina una tarea de estimación existente.
     * Devuelve la entidad borrada para que el Vigilante pueda leer su nombre.
     */
    @Auditable(
        accion = "BORRAR_ESTIMACION", 
        tabla = "detalle_estimacion", 
        entidad = DetalleEstimacion.class,
        descripcion = "Se eliminó la estimación de la tarea '#{#resultado.tarea}'"
    )
    public DetalleEstimacion eliminarTarea(Long id) {
        DetalleEstimacion detalle = detalleEstimacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró la tarea con ID: " + id));

        detalleEstimacionRepository.delete(detalle);
        
        return detalle; // Lo devolvemos para que SpEL pueda leer "resultado.tarea"
    }

} //<-- fin del servicio DetalleEstimacionService.java
