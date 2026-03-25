package com.example.demo.dto;

public class ApiResponse {
    private String[][] datos;
    private String mensaje;
    private boolean success;
    private Object data;

    public ApiResponse(String[][] datos, String mensaje, boolean success, Object data) {
        this.datos = datos;
        this.mensaje = mensaje;
        this.success = success;
        this.data = data;
    }

    // Getters
    public String[][] getDatos() { return datos;}
    public String getMensaje() { return mensaje; }
    public boolean isSuccess() { return success; }
    public Object getData() { return data; }
}
