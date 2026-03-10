package co.edu.escuelaing.reflexionlab.controllers;

import co.edu.escuelaing.reflexionlab.annotations.GetMapping;
import co.edu.escuelaing.reflexionlab.annotations.RequestParam;
import co.edu.escuelaing.reflexionlab.annotations.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Controlador adicional para demostrar que el framework carga
 * múltiples @RestController de forma automática.
 */
@RestController
public class TimeController {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/time")
    public String time(@RequestParam(value = "zone", defaultValue = "America/Bogota") String zone) {
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(zone);
        } catch (Exception e) {
            zoneId = ZoneId.of("America/Bogota");
        }
        String now = LocalDateTime.now(zoneId).format(FMT);
        return "<h1>🕐 Hora actual</h1>"
             + "<p><strong>" + now + "</strong> (" + zoneId + ")</p>"
             + "<p>Prueba con otra zona: <a href='/time?zone=Europe/Berlin'>/time?zone=Europe/Berlin</a></p>";
    }

    @GetMapping("/about")
    public String about() {
        return "<h1>MicroSpringBoot</h1>"
             + "<p>Framework IoC mínimo construido con reflexión Java.</p>"
             + "<h2>Capacidades demostradas:</h2>"
             + "<ul>"
             + "<li>✅ Escaneo automático de <code>@RestController</code> via reflexión</li>"
             + "<li>✅ Routing dinámico con <code>@GetMapping</code></li>"
             + "<li>✅ Inyección de parámetros con <code>@RequestParam</code></li>"
             + "<li>✅ Archivos estáticos (HTML, PNG, CSS)</li>"
             + "<li>✅ Múltiples solicitudes no concurrentes</li>"
             + "</ul>"
             + "<p><a href='/'>← Inicio</a></p>";
    }
}
