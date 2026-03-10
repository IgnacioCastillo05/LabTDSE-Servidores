package co.edu.escuelaing.reflexionlab.framework;

import co.edu.escuelaing.reflexionlab.annotations.GetMapping;
import co.edu.escuelaing.reflexionlab.annotations.RequestParam;
import co.edu.escuelaing.reflexionlab.annotations.RestController;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

/**
 * MicroSpringBoot: framework IoC mínimo basado en reflexión.
 *
 * Modo 1 - Clase explícita (línea de comandos):
 *   java -cp target/classes co.edu.escuelaing.reflexionlab.MicroSpringBoot co.edu.escuelaing.reflexionlab.FirstWebService
 *
 * Modo 2 - Escaneo automático del classpath:
 *   java -cp target/classes co.edu.escuelaing.reflexionlab.MicroSpringBoot
 *
 * El framework:
 *  1. Carga clases anotadas con @RestController (por argumento o por escaneo)
 *  2. Detecta métodos anotados con @GetMapping vía reflexión
 *  3. Registra las rutas en el servidor HTTP
 *  4. Resuelve @RequestParam inyectando los query params en cada invocación
 */
public class MicroSpringBoot {

    private static final Logger LOGGER = Logger.getLogger(MicroSpringBoot.class.getName());
    private static final int PORT = 35000;
    private static final String STATIC_ROOT = "src/main/resources/webroot/public";

    /** Mapa de rutas → (instancia del controlador, método a invocar) */
    private final Map<String, RouteEntry> routes = new HashMap<>();

    public static void main(String[] args) throws Exception {
        MicroSpringBoot app = new MicroSpringBoot();

        if (args.length > 0) {
            // Modo 1: clases explícitas en la línea de comandos
            for (String className : args) {
                app.loadController(className);
            }
        } else {
            // Modo 2: escaneo automático del classpath
            app.scanClasspath();
        }

        app.startServer();
    }

    /**
     * Carga un controlador por nombre completo de clase.
     * Usa Class.forName() + reflexión para inspeccionar sus métodos.
     */
    public void loadController(String className) throws Exception {
        LOGGER.info("Cargando controlador: " + className);
        Class<?> clazz = Class.forName(className);

        if (!clazz.isAnnotationPresent(RestController.class)) {
            LOGGER.warning("La clase " + className + " no tiene @RestController, ignorada.");
            return;
        }

        registerRoutes(clazz);
    }

    /**
     * Registra todas las rutas de una clase @RestController.
     * Itera sus métodos buscando @GetMapping.
     */
    private void registerRoutes(Class<?> clazz) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                String path = method.getAnnotation(GetMapping.class).value();
                routes.put(path, new RouteEntry(instance, method));
                LOGGER.info("  Ruta registrada: GET " + path + " → " + clazz.getSimpleName() + "." + method.getName() + "()");
            }
        }
    }

    /**
     * Escanea todas las clases en el classpath buscando @RestController.
     * Equivalente al component scan de Spring.
     */
    public void scanClasspath() throws Exception {
        LOGGER.info("Iniciando escaneo automático del classpath...");

        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(File.pathSeparator);

        for (String entry : entries) {
            File entryFile = new File(entry);
            if (entryFile.isDirectory()) {
                scanDirectory(entryFile, entryFile);
            }
        }

        LOGGER.info("Escaneo completado. Rutas registradas: " + routes.size());
    }

    private void scanDirectory(File baseDir, File currentDir) throws Exception {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(baseDir, file);
            } else if (file.getName().endsWith(".class")) {
                String className = toClassName(baseDir, file);
                tryLoadAsController(className);
            }
        }
    }

    private String toClassName(File baseDir, File classFile) {
        String basePath  = baseDir.getAbsolutePath();
        String classPath = classFile.getAbsolutePath();
        // Quitar el directorio base y la extensión .class
        String relative = classPath.substring(basePath.length() + 1, classPath.length() - 6);
        return relative.replace(File.separatorChar, '.');
    }

    private void tryLoadAsController(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(RestController.class)) {
                LOGGER.info("Encontrado @RestController: " + className);
                registerRoutes(clazz);
            }
        } catch (Throwable ignored) {
            // Clases del sistema o sin constructor accesible: se ignoran silenciosamente
        }
    }

    public void startServer() throws IOException {
        LOGGER.info("MicroSpringBoot arriba en http://localhost:" + PORT);
        LOGGER.info("Rutas activas: " + routes.keySet());

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                System.out.println("Esperando solicitud...");
                try (Socket client = serverSocket.accept()) {
                    handleRequest(client);
                } catch (Exception e) {
                    LOGGER.severe("Error procesando solicitud: " + e.getMessage());
                }
            }
        }
    }

    private void handleRequest(Socket client) throws Exception {
        BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream()));
        OutputStream   out = client.getOutputStream();

        // Leer línea de solicitud
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isBlank()) return;
        LOGGER.info("Recibido: " + requestLine);

        // Consumir headers
        while (true) {
            String line = in.readLine();
            if (line == null || line.isBlank()) break;
        }

        // Parsear: GET /path?query HTTP/1.1
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) { sendError(out, 400, "Bad Request"); return; }

        URI uri;
        try {
            uri = new URI(parts[1]);
        } catch (Exception e) {
            sendError(out, 400, "Bad Request");
            return;
        }

        String path  = uri.getPath();
        String query = uri.getQuery(); // puede ser null

        // 1) Buscar ruta registrada por el framework IoC (tiene prioridad)
        RouteEntry route = routes.get(path);
        
        // Si no hay ruta para "/" redirigir a index.html estático
        if (route == null && path.equals("/")) path = "/index.html";

        // 2) Intentar servir archivos estáticos
        if (route == null && serveStaticFile(out, path)) return;
        if (route != null) {
            String body = invokeRoute(route, query);
            sendHtml(out, 200, body);
            return;
        }

        sendError(out, 404, "No route found for: " + path);
    }


    /**
     * Invoca el método del controlador inyectando los @RequestParam del query string.
     * Aquí está el corazón de IoC: reflexión para resolver dependencias en tiempo de ejecución.
     */
    private String invokeRoute(RouteEntry route, String query) throws Exception {
        Method method = route.method;
        Parameter[] params = method.getParameters();

        // Parsear query string → mapa de parámetros
        Map<String, String> queryParams = parseQuery(query);

        // Construir los argumentos usando reflexión sobre las anotaciones de parámetros
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = params[i].getAnnotation(RequestParam.class);
                args[i] = queryParams.getOrDefault(rp.value(), rp.defaultValue());
            } else {
                args[i] = null;
            }
        }

        // Invocar el método vía reflexión
        Object result = method.invoke(route.instance, args);
        return result != null ? result.toString() : "";
    }

    private boolean serveStaticFile(OutputStream out, String path) throws IOException {
        File file = new File(STATIC_ROOT + path);
        if (!file.exists() || file.isDirectory()) return false;

        byte[] content    = Files.readAllBytes(file.toPath());
        String contentType = getContentType(file.getName());

        String header = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + content.length + "\r\n"
                + "\r\n";
        out.write(header.getBytes());
        out.write(content);
        out.flush();
        LOGGER.info("Archivo estático: " + file.getName() + " (" + contentType + ")");
        return true;
    }

    private void sendHtml(OutputStream out, int code, String body) throws IOException {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<title>MicroSpringBoot</title>"
                + "<style>body{font-family:sans-serif;max-width:800px;margin:40px auto;padding:20px;}"
                + "h1{color:#333;}p{color:#555;}</style>"
                + "</head><body>" + body + "</body></html>";
        byte[] content = html.getBytes("UTF-8");
        String header = "HTTP/1.1 " + code + " OK\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Length: " + content.length + "\r\n"
                + "\r\n";
        out.write(header.getBytes());
        out.write(content);
        out.flush();
    }

    private void sendError(OutputStream out, int code, String msg) throws IOException {
        String status = code == 404 ? "Not Found" : "Bad Request";
        String html = "<!DOCTYPE html><html><body><h1>" + code + " " + status + "</h1>"
                + "<p>" + msg + "</p><a href='/'>Inicio</a></body></html>";
        byte[] content = html.getBytes("UTF-8");
        String header = "HTTP/1.1 " + code + " " + status + "\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Length: " + content.length + "\r\n"
                + "\r\n";
        out.write(header.getBytes());
        out.write(content);
        out.flush();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return map;
    }

    private String getContentType(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".html")) return "text/html";
        if (name.endsWith(".css"))  return "text/css";
        if (name.endsWith(".js"))   return "application/javascript";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif"))  return "image/gif";
        if (name.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    private static class RouteEntry {
        final Object instance;
        final Method method;

        RouteEntry(Object instance, Method method) {
            this.instance = instance;
            this.method   = method;
        }
    }
}