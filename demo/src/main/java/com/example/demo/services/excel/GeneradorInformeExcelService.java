package com.example.demo.services.excel;

import java.io.ByteArrayInputStream;

import com.example.demo.dto.excel.CabeceraDTO;

public interface GeneradorInformeExcelService {
    
    ByteArrayInputStream generarExcelAnalitico(Long idProyecto, Integer idExcel);
    CabeceraDTO obtenerDatosCabecera(Long idProyecto, Integer idExcel);

}