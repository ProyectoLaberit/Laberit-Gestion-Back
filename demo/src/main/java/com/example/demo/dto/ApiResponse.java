package com.example.demo.dto;

public class ApiResponse {
    private String mensaje;
    private Boolean success;
    private Object data;

    public ApiResponse(String mensaje, Boolean success, Object data) {
        this.mensaje = mensaje;
        this.success = success;
        this.data = data;
    }

    // Getters
    public String getMensaje() { return mensaje; }
    public Boolean isSuccess() { return success; }
    public Object getData() { return data; }
}
