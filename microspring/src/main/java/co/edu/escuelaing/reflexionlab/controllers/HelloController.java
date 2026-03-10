package co.edu.escuelaing.reflexionlab.controllers;

import co.edu.escuelaing.reflexionlab.annotations.GetMapping;
import co.edu.escuelaing.reflexionlab.annotations.RestController;

/**
 * Ejemplo básico del enunciado del laboratorio.
 *
 *   @RestController
 *   public class HelloController {
 *       @GetMapping("/")
 *       public String index() { return "Greetings from Spring Boot!"; }
 *   }
 *
 * En modo escaneo automático (sin args) el framework lo detecta solo.
 */
@RestController
public class HelloController {

    @GetMapping("/greetings")
    public String index() {
        return "<h1>Greetings from MicroSpringBoot!</h1>"
             + "<p>Detectado automáticamente via escaneo del classpath.</p>";
    }
}
