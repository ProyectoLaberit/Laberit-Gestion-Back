package com.example.demo.services.excel;

import java.io.ByteArrayInputStream;

public interface GeneradorInformeExcelService {
    
    ByteArrayInputStream generarExcelAnalitico(Long idProyecto, Integer idExcelElegido);

}