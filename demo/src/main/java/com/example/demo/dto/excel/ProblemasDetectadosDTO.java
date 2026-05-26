package com.example.demo.dto.excel;

public class ProblemasDetectadosDTO {
    private Integer tareasGitlabNoReconocidas;
    private Integer imputacionesInvalidas;
    private Integer tareasSinHoras;

    public ProblemasDetectadosDTO(Integer tareasNoReconocidas, Integer imputacionesInvalidas, Integer tareasSinHoras) {
        this.tareasGitlabNoReconocidas = tareasNoReconocidas;
        this.imputacionesInvalidas = imputacionesInvalidas;
        this.tareasSinHoras = tareasSinHoras;
    }

    public Integer getTareasGitlabNoReconocidas() {
        return tareasGitlabNoReconocidas;
    }

    public void setTareasGitlabNoReconocidas(Integer tareasNoReconocidas) {
        this.tareasGitlabNoReconocidas = tareasNoReconocidas;
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
