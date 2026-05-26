package com.example.demo.dto.excel;

public class ProblemasDetectadosDTO {
    private Integer tareasNoReconocidas;
    private Integer imputacionesInvalidas;
    private Integer tareasSinHoras;

    public ProblemasDetectadosDTO(Integer tareasNoReconocidas, Integer imputacionesInvalidas, Integer tareasSinHoras) {
        this.tareasNoReconocidas = tareasNoReconocidas;
        this.imputacionesInvalidas = imputacionesInvalidas;
        this.tareasSinHoras = tareasSinHoras;
    }

    public Integer getTareasNoReconocidas() {
        return tareasNoReconocidas;
    }

    public void setTareasNoReconocidas(Integer tareasNoReconocidas) {
        this.tareasNoReconocidas = tareasNoReconocidas;
    }

    public Integer getImputacionesInvalidas() {
        return imputacionesInvalidas;
    }

    public void setImputacionesInvalidas(Integer imputacionesInvalidas) {
        this.imputacionesInvalidas = imputacionesInvalidas;
    }

    public Integer getTareasSinHoras() {
        return tareasSinHoras;
    }

    public void setTareasSinHoras(Integer tareasSinHoras) {
        this.tareasSinHoras = tareasSinHoras;
    }

}
