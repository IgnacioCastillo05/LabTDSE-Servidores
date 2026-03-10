package co.edu.escuelaing.reflexionlab;

import co.edu.escuelaing.reflexionlab.annotations.GetMapping;
import co.edu.escuelaing.reflexionlab.annotations.RestController;

/**
 * Controlador de ejemplo simple, usado para demostrar el modo 1
 * (clase explícita por línea de comandos).
 *
 * Invocación:
 *   java -cp target/classes co.edu.escuelaing.reflexionlab.framework.MicroSpringBoot \
 *        co.edu.escuelaing.reflexionlab.FirstWebService
 */
@RestController
public class FirstWebService {

    @GetMapping("/")
    public String index() {
        return "<h1>¡Greetings from MicroSpringBoot!</h1>"
             + "<p>Framework IoC basado en reflexión y anotaciones Java.</p>"
             + "<ul>"
             + "<li><a href='/hello'>/hello</a></li>"
             + "<li><a href='/greeting?name=Kike'>/greeting?name=Kike</a></li>"
             + "<li><a href='/time'>/time</a></li>"
             + "</ul>";
    }

    @GetMapping("/hello")
    public String hello() {
        return "<h1>Hello World!</h1><p>Ruta registrada por reflexión sobre @GetMapping.</p>";
    }
}
