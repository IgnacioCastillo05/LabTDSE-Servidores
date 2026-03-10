# LabTDSE-Servidores

## Descripción

Servidor web construido en Java que funciona de manera similar a Apache: es capaz de entregar páginas HTML e imágenes PNG a través de HTTP. Además, incluye **MicroSpringBoot**, un framework IoC mínimo que permite construir aplicaciones web a partir de POJOs usando reflexión y anotaciones personalizadas. El servidor atiende múltiples solicitudes de forma no concurrente.

## Arquitectura del proyecto

```
microspring/src/main/java/co/edu/escuelaing/reflexionlab/
├── annotations/
│   ├── RestController.java   # Marca una clase como componente web REST
│   ├── GetMapping.java        # Mapea un método a una ruta HTTP GET
│   └── RequestParam.java      # Vincula un query param a un parámetro del método
├── controllers/
│   ├── HelloController.java       # Ruta simple /greetings
│   ├── GreetingController.java    # /greeting con @RequestParam (name)
│   └── TimeController.java        # /time y /about con parámetros opcionales
├── FirstWebService.java           # Controlador de ejemplo para modo CLI
└── framework/
    └── MicroSpringBoot.java       # Núcleo del framework (servidor HTTP + IoC)
```

## ¿Cómo funciona?

1. **Anotaciones personalizadas**: Se definieron `@RestController`, `@GetMapping` y `@RequestParam` como anotaciones de retención en tiempo de ejecución (`RUNTIME`).

2. **Carga de controladores**: El framework soporta dos modos:
   - **Modo explícito**: Se pasa la clase como argumento por línea de comandos.
   - **Modo escaneo automático**: Sin argumentos, escanea el classpath buscando todas las clases anotadas con `@RestController`.

3. **Registro de rutas por reflexión**: Para cada controlador cargado, el framework instancia la clase y recorre sus métodos buscando `@GetMapping`. Cada ruta encontrada se registra en un mapa interno `path → (instancia, método)`.

4. **Inyección de parámetros**: Al recibir una petición, el framework parsea los query params de la URL y los inyecta en los parámetros del método anotados con `@RequestParam`, usando el `defaultValue` cuando no se proporciona el parámetro.

5. **Archivos estáticos**: Si la ruta solicitada no coincide con ninguna ruta registrada, el servidor intenta servir un archivo estático desde `src/main/resources/webroot/public/` (HTML, PNG, CSS, JS).

## Ejemplo de controlador

```java
@RestController
public class GreetingController {

    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hola " + name;
    }
}
```

## Cómo compilar y ejecutar

```bash
cd microspring

# Compilar
mvn clean compile

# Modo 1 - Clase explícita por línea de comandos:
java -cp target/classes co.edu.escuelaing.reflexionlab.framework.MicroSpringBoot co.edu.escuelaing.reflexionlab.FirstWebService

# Modo 2 - Escaneo automático del classpath (detecta todos los @RestController):
java -cp target/classes co.edu.escuelaing.reflexionlab.framework.MicroSpringBoot

# También se puede ejecutar con Maven:
mvn exec:java
```

El servidor se levanta en `http://localhost:35000`.

## Rutas disponibles

| Ruta | Controlador | Descripción |
|------|------------|-------------|
| `/` | Archivo estático | Página de inicio (`index.html`) |
| `/greetings` | HelloController | Saludo simple sin parámetros |
| `/greeting` | GreetingController | Saludo con `@RequestParam` (`?name=Kike`) |
| `/time` | TimeController | Hora actual con zona horaria configurable (`?zone=Europe/Berlin`) |
| `/about` | TimeController | Información sobre el framework |
| `/hello` | FirstWebService | Ruta de ejemplo (modo explícito) |

---

### Evidencias conexión con instancia AWS
1. Conexión con la instancia por SSH
![alt text](microspring/img/conexionInstancia.jpeg)

2. Pasar el zip de las clases compiladas por SSH
![alt text](microspring/img/classes.jpeg)

3. Pruebas
![alt text](microspring/img/greetings.jpeg)

![alt text](microspring/img/helloN.jpeg)

![alt text](microspring/img/time.jpeg)