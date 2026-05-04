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

import com.example.demo.dto.DetalleEstimacionDTO;
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

    /**
     * Procesa un archivo Excel físico, extrae las estimaciones y las guarda en BD.
     * * @param archivo El archivo MultipartFile subido desde el cliente.
     * @param proyectoId ID del proyecto al que pertenece la estimación.
     * @param usuarioId ID del usuario que sube el archivo.
     * @return El número total de tareas válidas insertadas en la base de datos.
     */
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
    /*
        * Crea una nueva tarea manualmente desde el Frontend, asignándola a un proyecto específico.

    */
    public DetalleEstimacion crearTareaManual(Long idProyecto, DetalleEstimacionDTO dto) {
        // 1. Obtener el Excel vigente del proyecto
        Excel excelVigente = excelService.obtenerExcelVigentePorProyecto(idProyecto);

        if (excelVigente == null) {
            throw new RuntimeException("El proyecto no tiene un Excel vigente asociado.");
        }

        // 2. Instanciar la nueva entidad
        DetalleEstimacion nuevaTarea = new DetalleEstimacion();

        // 3. Asignar los valores obligatorios
        nuevaTarea.setIdExcel(excelVigente.getIdExcel());
        nuevaTarea.setIdFase(dto.getIdFase()); // Recordamos que este es el ID de la subfase
        nuevaTarea.setIdDepartamento(dto.getIdDepartamento());
        nuevaTarea.setTarea(dto.getTarea());
        nuevaTarea.setTiempoMin(dto.getTiempoMin());
        nuevaTarea.setTiempoMax(dto.getTiempoMax());

        // 4. Asegurarnos de que los opcionales van a null (por seguridad)
        nuevaTarea.setTiempoReal(null);
        nuevaTarea.setNumeroGitlab(null);

        // 5. Guardar en base de datos y retornar
        return detalleEstimacionRepository.save(nuevaTarea);
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
            dto.setIdFase(entidad.getIdFase());
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
   public List<DetalleEstimacionDTO> obtenerDetallePorCriterios(Long idProyecto, Integer idSubfase, String nombreTarea) {
    // 1. Obtener el Excel vigente
    Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
    if (excel == null) {
        return new java.util.ArrayList<>();
    }

    // 2. Traer todas las estimaciones y los departamentos para cruzar nombres
    List<DetalleEstimacion> todasLasEstimaciones = detalleEstimacionRepository.findByIdExcel(excel.getIdExcel());
    List<com.example.demo.entity.Departamento> todosLosDeptos = departamentoRepository.findAll();

    // 3. Filtrar todas las filas que coincidan con la subfase y el nombre de la tarea
    return todasLasEstimaciones.stream()
            .filter(d -> d.getIdFase() != null && d.getIdFase().equals(idSubfase))
            .filter(d -> d.getTarea() != null && d.getTarea().equalsIgnoreCase(nombreTarea.trim()))
            .map(entidad -> {
                // Usamos tu clase DetalleEstimacionDTO
                DetalleEstimacionDTO dto = new DetalleEstimacionDTO();
                dto.setId(entidad.getId());
                dto.setIdExcel(entidad.getIdExcel());
                dto.setIdDepartamento(entidad.getIdDepartamento());
                dto.setIdFase(entidad.getIdFase());
                dto.setTarea(entidad.getTarea());
                dto.setTiempoMin(entidad.getTiempoMin());
                dto.setTiempoMax(entidad.getTiempoMax());

                // Buscamos el nombre del departamento en la lista para rellenar el DTO
             // Buscamos el nombre del departamento de forma segura
            // Sustituye esa línea por esta:
            String nombreDepto = todosLosDeptos.stream()
                .filter(d -> d.getId() == entidad.getIdDepartamento())
                .map(d -> d.getNombre())
                .findFirst()
                .orElse("Desconocido");
                        dto.setNombreDepartamento(nombreDepto);
                        return dto;
            })
            .collect(java.util.stream.Collectors.toList());
}

    public List<TareaSubfaseDTO> obtenerTareasSubfase(long idProyecto, Integer idSubfase){

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

            dto.setIdTarea(entry.getValue().get(0).getId());// ID de una de las tareas (podrían ser varias con el mismo nombre)

            // Sumamos todos los mínimos de este grupo
            double sumaMin = entry.getValue().stream().mapToDouble(DetalleEstimacion::getTiempoMin).sum();
            // Sumamos todos los máximos de este grupo
            double sumaMax = entry.getValue().stream().mapToDouble(DetalleEstimacion::getTiempoMax).sum();

            dto.setTiempoTotalMin(sumaMin);
            dto.setTiempoTotalMax(sumaMax);
            
            resultado.add(dto);
        }

        return resultado;
    }




} //<-- fin del servicio DetalleEstimacionService.java