package com.example.demo.dto.excel;

import java.util.Date;

public interface ErrorVinculacionClockifyDTO {
    String getUsuario();
    String getDescripcionOriginal();
    Date getFechaTrabajada();
    Double getHorasTrabajadas();
    Boolean getValida();
}