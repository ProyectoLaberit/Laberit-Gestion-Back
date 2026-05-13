package com.example.demo.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // Solo se puede poner encima de métodos
@Retention(RetentionPolicy.RUNTIME) // Se lee en tiempo de ejecución

public @interface Auditable {

    String accion();

    String tabla();

    Class<?> entidad();
}
