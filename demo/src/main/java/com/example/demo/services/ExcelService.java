package com.example.demo.services;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.annotation.Auditable;
import com.example.demo.dto.HistorialExcelDTO;
import com.example.demo.entity.Departamento;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.entity.Fase;
import com.example.demo.entity.TareaProyecto;
import com.example.demo.repository.DepartamentoRepository;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.repository.ExcelRepository;
import com.example.demo.repository.FaseRepository;
import com.example.demo.repository.TareaProyectoRepository;

@Service
public class ExcelService {

    @Autowired
    private ExcelRepository excelRepository;

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private FaseRepository faseRepository;

    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

    @Auditable(
        accion = "NUEVO_EXCEL_VIGENTE", 
        tabla = "excel", 
        entidad = Excel.class,
        descripcion = "Se ha establecido un nuevo Excel vigente para el proyecto con ID: #{#excel.idProyecto}"
    )
    @Transactional
    public Excel guardarDatosExcel(Excel excel) {
        if (excel.getIdProyecto() != null) {
            excelRepository.desactivarExcelsAnteriores(excel.getIdProyecto());
        }
        return excelRepository.save(excel);
    }

    public int crearYGuardarExcel(List<DetalleEstimacion> datos) {
        if (datos == null || datos.isEmpty()) {
            return 0;
        }
        return datos.size();
    }

    public Excel obtenerExcelVigentePorProyecto(Long idProyecto) {
        return excelRepository.findFirstByIdProyectoAndVigenteTrue(idProyecto);
    }

    public List<HistorialExcelDTO> obtenerHistorialExcels(Long proyectoId) {
        return excelRepository.obtenerHistorialDirecto(proyectoId);
    }

    /**
     * Coordina el proceso de exportación completo mediante una estrategia de doble pasada.
     * Carga las colecciones iniciales y delega las fases de inyección a métodos especializados.
     * @param idExcel ID del documento exacto del historial a descargar.
     * @return byte[] El contenido binario del archivo generado.
     */
    @Auditable(
        accion = "DESCARGAR_EXCEL", 
        tabla = "excel", 
        entidad = Excel.class,
        descripcion = "Se descargó el Excel con ID: #idExcel"
    )
    public byte[] exportarExcelCompleto(Integer idExcel) throws Exception {
        InputStream is = getClass().getResourceAsStream("/templates/PlantillaBase.xlsx");
        if (is == null) {
            throw new RuntimeException("No se encontró el archivo PlantillaBase.xlsx en src/main/resources/templates/");
        }
        
        Workbook workbook = WorkbookFactory.create(is);
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        
        List<DetalleEstimacion> detalles = detalleEstimacionRepository.findByIdExcel(idExcel);
        List<Departamento> departamentos = departamentoRepository.findAll();
        List<Fase> fases = faseRepository.findAll();

        // Extraemos el Excel base para obtener el Proyecto y cargar el pivote TareaProyecto
        Excel excel = excelRepository.findById(idExcel)
            .orElseThrow(() -> new RuntimeException("Excel no encontrado"));
            
        List<TareaProyecto> tareasPivot = tareaProyectoRepository.findAll().stream()
                .filter(t -> t.getIdProyecto().equals(excel.getIdProyecto()))
                .collect(Collectors.toList());
        
        // Creamos un diccionario en memoria para cruzar rápido los datos sin saturar la BD
        Map<Long, TareaProyecto> mapaTareas = tareasPivot.stream()
                .collect(Collectors.toMap(TareaProyecto::getId, t -> t, (a, b) -> a));
        
        Sheet hoja = workbook.getSheet("Propuesta actualizada");
        
        Map<Integer, Integer[]> columnasPorDepto = obtenerMapeoColumnas(departamentos);
        Map<String, Integer> mapaFases = obtenerMapeoFases(fases);
        Map<Integer, Map<String, List<DetalleEstimacion>>> recamaraTareas = agruparTareasPorSubfase(detalles, mapaTareas);

        // Primera Pasada: Coincidencias Exactas (Francotirador)
        procesarNombresExactos(hoja, mapaFases, columnasPorDepto, recamaraTareas, mapaTareas);

        // Segunda Pasada: Relleno de Huecos Vacíos/Genéricos (Cola de inyección)
        procesarHuecosEnBlanco(hoja, mapaFases, columnasPorDepto, recamaraTareas, mapaTareas);

        evaluator.evaluateAll();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        is.close();
        
        return bos.toByteArray();
    }

    /**
     * Genera un mapa indexando los identificadores de departamento con sus respectivas columnas en Excel.
     * @param departamentos Lista de todos los departamentos en la base de datos.
     * @return Map con el ID del departamento y un array con los índices de las columnas [Min, Max].
     */
    private Map<Integer, Integer[]> obtenerMapeoColumnas(List<Departamento> departamentos) {
        Map<Integer, Integer[]> columnasPorDepto = new HashMap<>();
        for (Departamento depto : departamentos) {
            String nombreLimpio = normalizarTextoAuxiliar(depto.getNombre());
            if (nombreLimpio.equals("comercial")) { columnasPorDepto.put(depto.getId(), new Integer[]{3, 4}); }
            else if (nombreLimpio.equals("direccion")) { columnasPorDepto.put(depto.getId(), new Integer[]{5, 6}); }
            else if (nombreLimpio.equals("back")) { columnasPorDepto.put(depto.getId(), new Integer[]{7, 8}); }
            else if (nombreLimpio.equals("front")) { columnasPorDepto.put(depto.getId(), new Integer[]{9, 10}); }
            else if (nombreLimpio.equals("soporte")) { columnasPorDepto.put(depto.getId(), new Integer[]{11, 12}); }
            else if (nombreLimpio.equals("mk")) { columnasPorDepto.put(depto.getId(), new Integer[]{13, 14}); }
            else if (nombreLimpio.equals("ux")) { columnasPorDepto.put(depto.getId(), new Integer[]{15, 16}); }
            else if (nombreLimpio.equals("ui")) { columnasPorDepto.put(depto.getId(), new Integer[]{17, 18}); }
            else if (nombreLimpio.equals("wp-maq")) { columnasPorDepto.put(depto.getId(), new Integer[]{19, 20}); }
        }
        return columnasPorDepto;
    }

    /**
     * Genera un mapa indexando los nombres normalizados de las fases con sus respectivos IDs únicos.
     * @param fases Lista de todas las fases de la base de datos.
     * @return Map con el nombre normalizado como clave y el ID de la fase como valor.
     */
    private Map<String, Integer> obtenerMapeoFases(List<Fase> fases) {
        Map<String, Integer> mapaFases = new HashMap<>();
        for (Fase fase : fases) {
            mapaFases.put(normalizarTextoAuxiliar(fase.getNombre()), fase.getId());
        }
        return mapaFases;
    }

    /**
     * Organiza y agrupa la lista de estimaciones en una estructura indexada en memoria (3D).
     * Agrupa por ID de subfase y asocia todas las filas de departamentos correspondientes a una tarea única.
     * @param detalles Lista de estimaciones obtenidas del idExcel específico.
     * @param mapaTareas Diccionario de resolución del pivote central.
     * @return Un mapa anidado estructurado por ID de Subfase y Nombre de Tarea normalizado.
     */
    private Map<Integer, Map<String, List<DetalleEstimacion>>> agruparTareasPorSubfase(List<DetalleEstimacion> detalles, Map<Long, TareaProyecto> mapaTareas) {
        Map<Integer, Map<String, List<DetalleEstimacion>>> recamara = new HashMap<>();
        for (DetalleEstimacion det : detalles) {
            TareaProyecto tp = mapaTareas.get(det.getIdTareaProyecto());
            
            if (tp == null || tp.getIdFase() == null || tp.getTarea() == null) {
                continue;
            }
            
            if (!recamara.containsKey(tp.getIdFase())) {
                recamara.put(tp.getIdFase(), new HashMap<>());
            }
            
            String claveTarea = normalizarTextoAuxiliar(tp.getTarea());
            if (!recamara.get(tp.getIdFase()).containsKey(claveTarea)) {
                recamara.get(tp.getIdFase()).put(claveTarea, new ArrayList<>());
            }
            
            recamara.get(tp.getIdFase()).get(claveTarea).add(det);
        }
        return recamara;
    }

    /**
     * Primera Pasada: Recorre la plantilla buscando coincidencias exactas por nombre de tarea e inyecta los tiempos correspondientes.
     * Elimina las tareas encontradas de la recámara para evitar duplicados futuros.
     * @param hoja La pestaña del libro Excel a procesar.
     * @param mapaFases Mapa de resolución de IDs de fases.
     * @param columnasPorDepto Coordenadas de columnas de los departamentos.
     * @param recamaraTareas Estructura con las tareas pendientes por subfase.
     * @param mapaTareas Diccionario de resolución del pivote central.
     */
    private void procesarNombresExactos(Sheet hoja, Map<String, Integer> mapaFases, Map<Integer, Integer[]> columnasPorDepto, Map<Integer, Map<String, List<DetalleEstimacion>>> recamaraTareas, Map<Long, TareaProyecto> mapaTareas) {
        Integer idSubfaseActual = null;
        int totalFilas = hoja.getLastRowNum();

        for (int i = 4; i <= totalFilas; i++) {
            Row fila = hoja.getRow(i);
            if (fila == null) {
                continue;
            }

            Cell celdaSubfase = fila.getCell(1);
            if (celdaSubfase != null && !celdaSubfase.toString().trim().isEmpty()) {
                String subfaseLeida = normalizarTextoAuxiliar(celdaSubfase.toString());
                if (mapaFases.containsKey(subfaseLeida)) {
                    idSubfaseActual = mapaFases.get(subfaseLeida);
                }
            }

            Cell celdaTarea = fila.getCell(2);
            if (celdaTarea == null || celdaTarea.toString().trim().isEmpty() || idSubfaseActual == null) {
                continue;
            }

            String nombreTareaExcel = celdaTarea.toString().trim();
            String claveTarea = normalizarTextoAuxiliar(nombreTareaExcel);

            if (recamaraTareas.containsKey(idSubfaseActual) && recamaraTareas.get(idSubfaseActual).containsKey(claveTarea)) {
                List<DetalleEstimacion> listaDeptos = recamaraTareas.get(idSubfaseActual).get(claveTarea);
                for (DetalleEstimacion det : listaDeptos) {
                    TareaProyecto tp = mapaTareas.get(det.getIdTareaProyecto());
                    if (tp == null) {
                        continue;
                    }
                    
                    Integer[] columnas = columnasPorDepto.get(tp.getIdDepartamento());
                    if (columnas != null) {
                        Cell celdaMin = fila.getCell(columnas[0], MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        celdaMin.setCellValue(det.getTiempoMin() != null ? det.getTiempoMin() : 0.0);
                        
                        Cell celdaMax = fila.getCell(columnas[1], MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        celdaMax.setCellValue(det.getTiempoMax() != null ? det.getTiempoMax() : 0.0);
                    }
                }
                recamaraTareas.get(idSubfaseActual).remove(claveTarea);
            }
        }
    }

    /**
     * Segunda Pasada: Identifica celdas vacías o con nombres genéricos ("Funcionalidad X") e inyecta secuencialmente las tareas restantes.
     * Sobrescribe el nombre de la celda de la tarea en el archivo resultante con el nombre real de la base de datos.
     * @param hoja La pestaña del libro Excel a procesar.
     * @param mapaFases Mapa de resolución de IDs de fases.
     * @param columnasPorDepto Coordenadas de columnas de los departamentos.
     * @param recamaraTareas Estructura con las tareas sobrantes en memoria.
     * @param mapaTareas Diccionario de resolución del pivote central.
     */
    private void procesarHuecosEnBlanco(Sheet hoja, Map<String, Integer> mapaFases, Map<Integer, Integer[]> columnasPorDepto, Map<Integer, Map<String, List<DetalleEstimacion>>> recamaraTareas, Map<Long, TareaProyecto> mapaTareas) {
        Integer idSubfaseActual = null;
        int totalFilas = hoja.getLastRowNum();

        for (int i = 4; i <= totalFilas; i++) {
            Row fila = hoja.getRow(i);
            if (fila == null) {
                continue;
            }

            Cell celdaSubfase = fila.getCell(1);
            if (celdaSubfase != null && !celdaSubfase.toString().trim().isEmpty()) {
                String subfaseLeida = normalizarTextoAuxiliar(celdaSubfase.toString());
                if (mapaFases.containsKey(subfaseLeida)) {
                    idSubfaseActual = mapaFases.get(subfaseLeida);
                }
            }

            if (idSubfaseActual == null || !recamaraTareas.containsKey(idSubfaseActual) || recamaraTareas.get(idSubfaseActual).isEmpty()) {
                continue;
            }

            Cell celdaTarea = fila.getCell(2, MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String textoTarea = celdaTarea.toString().trim();
            String textoNormalizado = normalizarTextoAuxiliar(textoTarea);

            if (textoTarea.isEmpty() || textoNormalizado.startsWith("funcionalidad")) {
                Iterator<String> it = recamaraTareas.get(idSubfaseActual).keySet().iterator();
                if (it.hasNext()) {
                    String primeraClave = it.next();
                    List<DetalleEstimacion> listaDeptos = recamaraTareas.get(idSubfaseActual).get(primeraClave);
                    
                    if (!listaDeptos.isEmpty()) {
                        TareaProyecto tpPrimero = mapaTareas.get(listaDeptos.get(0).getIdTareaProyecto());
                        if (tpPrimero != null) {
                            celdaTarea.setCellValue(tpPrimero.getTarea());
                        }
                    }

                    for (DetalleEstimacion det : listaDeptos) {
                        TareaProyecto tp = mapaTareas.get(det.getIdTareaProyecto());
                        if (tp == null) {
                            continue;
                        }
                        
                        Integer[] columnas = columnasPorDepto.get(tp.getIdDepartamento());
                        if (columnas != null) {
                            Cell celdaMin = fila.getCell(columnas[0], MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            celdaMin.setCellValue(det.getTiempoMin() != null ? det.getTiempoMin() : 0.0);
                            
                            Cell celdaMax = fila.getCell(columnas[1], MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            celdaMax.setCellValue(det.getTiempoMax() != null ? det.getTiempoMax() : 0.0);
                        }
                    }
                    it.remove();
                }
            }
        }
    }

    /**
     * Limpia y normaliza un texto: elimina espacios adicionales, tildes y lo convierte a minúsculas.
     * @param texto El texto original a limpiar.
     * @return El texto normalizado resultante.
     */
    private String normalizarTextoAuxiliar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }
        String limpio = texto.trim().toLowerCase();
        String normalizado = Normalizer.normalize(limpio, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}