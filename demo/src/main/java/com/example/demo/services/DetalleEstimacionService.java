package com.example.demo.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.DetalleEstimacionDTO;
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

    public int procesarExcel(MultipartFile archivo, long proyectoId, Integer usuarioId) throws Exception {
        // 1. Guardar el Excel en la BD (Esto lo tenías bien)
        Excel registroExcel = new Excel();
        registroExcel.setIdProyecto(proyectoId);
        registroExcel.setIdUsuario(usuarioId);
        registroExcel.setFechaSubida(LocalDate.now());
        registroExcel.setRutaArchivo("uploads/" + archivo.getOriginalFilename());
        
        Excel excelGuardado = excelService.guardarDatosExcel(registroExcel);
        Integer idExcelGenerado = excelGuardado.getIdExcel();

        List<DetalleEstimacion> listaParaGuardar = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(archivo.getInputStream());
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();

        // --- LA MAGIA: EL DICCIONARIO EN MEMORIA ---
        // Nos traemos TODAS las fases de la BD de una sola vez
        List<Fase> todasLasFasesBD = faseRepository.findAll();

        // 2. Mapeo de columnas (Esto también lo tenías bien)
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
        
        // --- ESTADOS DE MEMORIA JERÁRQUICA ---
        Integer idFasePadreActual = null;
        Integer idSubfaseActual = null;
        String nombreTareaActual = null; 

        // 3. Bucle principal de lectura
        for (Row fila : hoja) {
            if (fila.getRowNum() < 4) { continue; }
            if (esFilaFinal(fila)) { break; }

            // A. EVALUAR CAMBIO DE ESTADO: FASE PADRE (Columna 0)
            String valFase = formatter.formatCellValue(fila.getCell(0));
            String faseLimpia = normalizarTexto(valFase);
            
            if (!faseLimpia.isEmpty()) {
                // Buscamos en nuestra memoria (todasLasFasesBD) en lugar de en el repositorio
                idFasePadreActual = todasLasFasesBD.stream()
                    .filter(f -> f.getFasePadre() == null && normalizarTexto(f.getNombre()).equals(faseLimpia))
                    .map(Fase::getId)
                    .findFirst()
                    .orElse(null);
                
                idSubfaseActual = null; 
                nombreTareaActual = null; 
            }

            // B. EVALUAR CAMBIO DE ESTADO: SUBFASE (Columna 1)
            String valSubfase = formatter.formatCellValue(fila.getCell(1));
            String subfaseLimpia = normalizarTexto(valSubfase);
            
            if (!subfaseLimpia.isEmpty() && idFasePadreActual != null) {
                final Integer idPadre = idFasePadreActual; // Necesario para el lambda
                idSubfaseActual = todasLasFasesBD.stream()
                    .filter(f -> f.getFasePadre() != null && f.getFasePadre().equals(idPadre) && normalizarTexto(f.getNombre()).equals(subfaseLimpia))
                    .map(Fase::getId)
                    .findFirst()
                    .orElse(null);
                
                nombreTareaActual = null; 
            }

           // C. EVALUAR CAMBIO DE ESTADO: TAREA (Columna 2)
            String valTarea = formatter.formatCellValue(fila.getCell(2)).trim();
            
            if (!valTarea.isEmpty() && !normalizarTexto(valTarea).equals("analisis")) {
                nombreTareaActual = valTarea;
            } else if (valTarea.isEmpty() && idSubfaseActual != null) {
                
                // SOLUCIÓN: Creamos una copia congelada (final) para que el lambda no se queje
                final Integer idSubfaseParaLambda = idSubfaseActual;
                
                // Si la Tarea está vacía, hereda automáticamente el nombre de la Subfase
                nombreTareaActual = todasLasFasesBD.stream()
                    .filter(f -> f.getId().equals(idSubfaseParaLambda))
                    .map(Fase::getNombre)
                    .findFirst()
                    .orElse("Sin Tarea");
            }

            // 4. PROCESAMIENTO DE DATOS MATRICIALES (Solo si hay Subfase y Tarea en memoria)
            if (idSubfaseActual != null && nombreTareaActual != null) {
                for (RangoDepartamento depto : mapaColumnas) {
                    if (depto.getIdBD() == null || depto.getIdBD() == -1) {
                        continue;
                    }

                    Double min = extraerNumeroDeCelda(fila.getCell(depto.getColMin()), evaluator);
                    Double max = extraerNumeroDeCelda(fila.getCell(depto.getColMax()), evaluator);

                    // FILTRO CORREGIDO: Guardamos si la celda NO está vacía (acepta los 0 literales)
                    if (min != null || max != null) {
                        DetalleEstimacion detalle = new DetalleEstimacion();
                        detalle.setIdExcel(idExcelGenerado);
                        detalle.setIdDepartamento(depto.getIdBD());
                        detalle.setIdFase(idSubfaseActual);
                        detalle.setTarea(nombreTareaActual);
                        
                        // Si uno de los dos está vacío pero el otro tiene valor, le ponemos 0.0 al vacío
                        detalle.setTiempoMin(min != null ? min : 0.0);
                        detalle.setTiempoMax(max != null ? max : 0.0);
                        
                        listaParaGuardar.add(detalle);
                    }
                }
            }
        } // Fin del bucle for de filas

        workbook.close();
        if (!listaParaGuardar.isEmpty()) {
            detalleEstimacionRepository.saveAll(listaParaGuardar);
        }
        return listaParaGuardar.size();
    } // Fin del método procesarExcel


  // ==========================================================
    // MÉTODOS AUXILIARES Y NORMALIZACIÓN
    // ==========================================================

    /**
     * Limpia un texto: quita espacios a los lados, lo pasa a minúsculas y elimina tildes.
     */
    private String normalizarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }
        String limpio = texto.trim().toLowerCase();
        String normalizado = java.text.Normalizer.normalize(limpio, java.text.Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private int determinarDepartamento(String nombre) {
        String nombreLimpio = normalizarTexto(nombre);
        return departamentoRepository.findAll().stream()
                .filter(d -> normalizarTexto(d.getNombre()).equals(nombreLimpio))
                .map(d -> d.getId())
                .findFirst()
                .orElse(-1);
    }

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

    private boolean esFilaFinal(Row fila) {
        if (fila == null) { return true; }
        Cell cellA = fila.getCell(0);
        return cellA != null && normalizarTexto(cellA.toString()).contains("total");
    }

    // ==========================================================
    // MÉTODOS DE CONSULTA Y EXPORTACIÓN
    // ==========================================================

   public List<DetalleEstimacionDTO> obtenerDetallesPorExcel(Integer idExcel) {
        // 1. Nos traemos los datos en bruto de la estimación
        List<DetalleEstimacion> detalles = detalleEstimacionRepository.findByIdExcel(idExcel);
        
        // 2. Diccionarios en memoria (Una sola consulta a BD para no saturar)
        List<Fase> todasLasFases = faseRepository.findAll();
        List<Departamento> todosLosDeptos = departamentoRepository.findAll();

        // 3. Mapeo y cruce de datos
        return detalles.stream().map(entidad -> {
            DetalleEstimacionDTO dto = new DetalleEstimacionDTO();
            dto.setId(entidad.getId()); 
            dto.setIdExcel(entidad.getIdExcel());
            dto.setIdDepartamento(entidad.getIdDepartamento());
            dto.setIdFase(entidad.getIdFase());
            dto.setTarea(entidad.getTarea());
            dto.setTiempoMin(entidad.getTiempoMin());
            dto.setTiempoMax(entidad.getTiempoMax());

           // A. Traducir el Departamento
            String nombreDepto = todosLosDeptos.stream()
                    .filter(d -> d.getId() == entidad.getIdDepartamento()) // ¡Usamos == para el int!
                    .map(d -> d.getNombre())
                    .findFirst()
                    .orElse("Desconocido");
            dto.setNombreDepartamento(nombreDepto);

            // B. Traducir Subfase y Fase Padre
            Fase subfase = todasLasFases.stream()
                    .filter(f -> f.getId() == entidad.getIdFase()) // ¡Usamos == para el int!
                    .findFirst()
                    .orElse(null);

            if (subfase != null) {
                dto.setNombreSubfase(subfase.getNombre());

                // Buscamos a su padre usando el ID de fasePadre
                if (subfase.getFasePadre() != null) {
                    String nombrePadre = todasLasFases.stream()
                            .filter(f -> f.getId().equals(subfase.getFasePadre()))
                            .map(f -> f.getNombre())
                            .findFirst()
                            .orElse("Desconocido");
                    dto.setNombreFase(nombrePadre);
                } else {
                    // Por si acaso se guardó directamente en un padre
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

    public List<DetalleEstimacion> obtenerDetallesEntidadPorExcel(Integer idExcel) {
        return detalleEstimacionRepository.findByIdExcel(idExcel);
    }

    /**
     * Busca una estimación puntual usando los IDs directos.
     */
    public DetalleEstimacionDTO obtenerDetallePorCriterios(Long idProyecto, Integer idSubfase, String nombreTarea) {
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        if (excel == null) { return null; }

        // Búsqueda directa y limpia con el ID de la subfase
        DetalleEstimacion entidad = detalleEstimacionRepository.findFirstByIdExcelAndIdFaseAndTareaIgnoreCase(
                excel.getIdExcel(), idSubfase, nombreTarea.trim());

        if (entidad == null) { return null; }

        return new DetalleEstimacionDTO(         
            entidad.getId(),
            entidad.getIdExcel(),
            entidad.getIdDepartamento(),
            entidad.getIdFase(),
            null, // nombreDepartamento (no hace falta para esta consulta rápida)
            null, // nombreFase
            null, // nombreSubfase
            entidad.getTarea(),
            entidad.getTiempoMin(),
            entidad.getTiempoMax()
        );
    }
   
} // <-- Fin de la clase DetalleEstimacionService -->