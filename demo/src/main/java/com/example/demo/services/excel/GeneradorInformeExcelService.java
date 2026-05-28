package com.example.demo.services.excel;

import java.io.ByteArrayInputStream;
import java.util.List;

import com.example.demo.dto.excel.CabeceraDTO;
import com.example.demo.dto.excel.FilaAuditoriaClockifyDTO;
import com.example.demo.dto.excel.FilaValidacionGitlabDTO;
import com.example.demo.dto.excel.ResumenValidacionDTO;

public interface GeneradorInformeExcelService {
    
    ByteArrayInputStream generarExcelAnalitico(Long idProyecto, Integer idExcel);
    CabeceraDTO obtenerDatosCabecera(Long idProyecto, Integer idExcel);
    ResumenValidacionDTO obtenerResumenValidacion(Long idProyecto);
    List<FilaValidacionGitlabDTO> obtenerFilasValidacionGitlab(Long idProyecto);
    List<FilaAuditoriaClockifyDTO> obtenerFilasAuditoriaClockify(Long idProyecto);

}