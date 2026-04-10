package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "api")
public class ApiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_api")
    private Integer id;

    private String nombre;
    private String clave;

    @Column(name = "url_testeo")
    private String urlTesteo;

    @Column(name = "url_real")
    private String urlReal;

    // Getters
    public String getClave() { return clave; }
    public String getUrlReal() { return urlReal; }
    public String getNombre() { return nombre; }
}
