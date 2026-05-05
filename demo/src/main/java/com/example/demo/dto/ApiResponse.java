package com.example.demo.dto;

/**
 * Clase que se envia al front y contiene un mensaje, si ha funcionado y los datos enviados
 */
public class ApiResponse {
    private String mensaje;
    private boolean success;
    private Object data;

    public ApiResponse(String mensaje, boolean success, Object data) {
        this.mensaje = mensaje;
        this.success = success;
        this.data = data;
    }

    // Getters
    public String getMensaje() { return mensaje; }
    public boolean isSuccess() { return success; }
    public Object getData() { return data; }
}
