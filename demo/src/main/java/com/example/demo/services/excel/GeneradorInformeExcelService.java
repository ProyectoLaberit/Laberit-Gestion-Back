package com.example.demo.services.excel;

import java.io.ByteArrayInputStream;

import com.example.demo.dto.excel.CabeceraDTO;
import com.example.demo.dto.excel.FilaGitLabTareaDTO;
import java.util.List;

public interface GeneradorInformeExcelService {
    
    ByteArrayInputStream generarExcelAnalitico(Long idProyecto, Integer idExcel);
    CabeceraDTO obtenerDatosCabecera(Long idProyecto, Integer idExcel);

    List<FilaGitLabTareaDTO> obtenerTareasEstructuradas(Long idProyecto, Integer idExcel);
}