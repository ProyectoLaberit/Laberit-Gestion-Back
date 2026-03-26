package com.example.demo.controller;

// Importa tu propia clase ProyectoDTO para que el controlador sepa cómo es el objeto que debe enviar
import com.example.demo.dto.ProyectoDTO;

import com.example.demo.entity.Proyecto; // Importamos la Entity (la tabla)
import com.example.demo.repository.ProyectoRepository; // Importamos el mando a distancia
import org.springframework.beans.factory.annotation.Autowired; // Para la "inyección"

// Importa la herramienta para decir que un método responde a una petición "GET" (pedir datos)
import org.springframework.web.bind.annotation.GetMapping;

// Importa la herramienta para definir la dirección (URL) base de este controlador
import org.springframework.web.bind.annotation.RequestMapping;

// Importa la herramienta que convierte esta clase en un controlador de tipo REST (capaz de enviar JSON)
import org.springframework.web.bind.annotation.RestController;

// Importa la herramienta para permitir que aplicaciones externas (como tu Front-end) accedan a esta API
import org.springframework.web.bind.annotation.CrossOrigin;

// Utilidades estándar de Java para manejar listas de objetos
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
/* * Indica que esta clase es un controlador de API. 
 * Automáticamente convierte los objetos que devuelvas (como ProyectoDTO) 
 * al formato JSON, que es el lenguaje que entiende el navegador.
 */

@RequestMapping("/api/proyecto")
/* * Define la "ruta" principal. Para llamar a este código desde el navegador, 
 * tendrás que usar la dirección: http://localhost:8080/api/proyectos
 */

@CrossOrigin(origins = "*")
/* * "CORS" es una medida de seguridad. Al poner el asterisco (*), le dices a Spring 
 * que permita que CUALQUIER página (como tu Front-end en Angular o React) 
 * pueda pedirle datos a este servidor sin que el navegador lo bloquee.
 */


public class ProyectoController {

    @Autowired
    /* * @Autowired: Es la "magia" de Spring. 
     * Le dice al programa: "Busca el ProyectoRepository que creamos y conéctalo aquí".
     * Así podemos usarlo para hablar con Neon sin tener que crear el objeto a mano.
     */
    private ProyectoRepository proyectoRepository;
    
    @GetMapping("/lista")
    /*
     * @GetMapping: Es una etiqueta que dice: "Este método se activa cuando alguien 
     * pide datos (petición GET) a través del navegador".
     * * "/lista": Es el nombre que tú eliges para esta acción concreta. No es un archivo.
     * La ruta completa para ver esto en el navegador será: 
     * http://localhost:8080/api/proyectos/lista
     */
    public List<ProyectoDTO> obtenerProyectos() {
        List<Proyecto> proyectosDB = proyectoRepository.findAll();

        return proyectosDB.stream().map(p -> new ProyectoDTO(
            p.getNombre(),
            p.getDescripcion(),
            p.getFechaInicio(),
            p.isActivo()
        )).collect(Collectors.toList());
    }

}
