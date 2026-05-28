package com.example.demo.dto.excel;

public class ResumenValidacionDTO {

    private int totalTareasGitlab;
    private int tareasGitlabOk;
    private int tareasGitlabHuerfanas;
    private int totalImputacionesClockify;
    private int imputacionesClockifyOk;
    private int imputacionesClockifyErroneas;

    public ResumenValidacionDTO() {
    }

    public ResumenValidacionDTO(int totalTareasGitlab, int tareasGitlabOk, int tareasGitlabHuerfanas,
            int totalImputacionesClockify, int imputacionesClockifyOk, int imputacionesClockifyErroneas) {
        this.totalTareasGitlab = totalTareasGitlab;
        this.tareasGitlabOk = tareasGitlabOk;
        this.tareasGitlabHuerfanas = tareasGitlabHuerfanas;
        this.totalImputacionesClockify = totalImputacionesClockify;
        this.imputacionesClockifyOk = imputacionesClockifyOk;
        this.imputacionesClockifyErroneas = imputacionesClockifyErroneas;
    }

    public int getTotalTareasGitlab() {
        return totalTareasGitlab;
    }

    public void setTotalTareasGitlab(int totalTareasGitlab) {
        this.totalTareasGitlab = totalTareasGitlab;
    }

    public int getTareasGitlabOk() {
        return tareasGitlabOk;
    }

    public void setTareasGitlabOk(int tareasGitlabOk) {
        this.tareasGitlabOk = tareasGitlabOk;
    }

    public int getTareasGitlabHuerfanas() {
        return tareasGitlabHuerfanas;
    }

    public void setTareasGitlabHuerfanas(int tareasGitlabHuerfanas) {
        this.tareasGitlabHuerfanas = tareasGitlabHuerfanas;
    }

    public int getTotalImputacionesClockify() {
        return totalImputacionesClockify;
    }

    public void setTotalImputacionesClockify(int totalImputacionesClockify) {
        this.totalImputacionesClockify = totalImputacionesClockify;
    }

    public int getImputacionesClockifyOk() {
        return imputacionesClockifyOk;
    }

    public void setImputacionesClockifyOk(int imputacionesClockifyOk) {
        this.imputacionesClockifyOk = imputacionesClockifyOk;
    }

    public int getImputacionesClockifyErroneas() {
        return imputacionesClockifyErroneas;
    }

    public void setImputacionesClockifyErroneas(int imputacionesClockifyErroneas) {
        this.imputacionesClockifyErroneas = imputacionesClockifyErroneas;
    }
}