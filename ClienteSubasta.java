package cliente;

import comun.Mensaje;
import java.io.*;
import java.net.Socket;
import java.net.ConnectException;
import java.util.Scanner;

// Clase principal del cliente de subastas.
// Conecta al servidor mediante TCP, lanza un hilo receptor de notificaciones
// y muestra un menu interactivo al usuario por consola.
public class ClienteSubasta {

    // Direccion del servidor (puede cambiarse por argumento)
    private static final String HOST_DEFECTO = "localhost";
    private static final int PUERTO = 9090;

    // Socket y flujos de comunicacion con el servidor
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;

    // Hilo que recibe notificaciones asincronas del servidor
    private ReceptorNotificaciones receptor;

    // Lector de la entrada del usuario por consola
    private final Scanner scanner = new Scanner(System.in);

    // Nombre del usuario registrado en esta sesion
    private String nombreUsuario = null;

    // Indica si el cliente esta activo
    private boolean activo = true;

    public static void main(String[] args) {
        // Permitir pasar el host por argumento: java ClienteSubasta 192.168.1.10
        String host = (args.length > 0) ? args[0] : HOST_DEFECTO;

        ClienteSubasta cliente = new ClienteSubasta();
        cliente.iniciar(host);
    }

    // Conecta al servidor y lanza el menu principal
    public void iniciar(String host) {
        System.out.println("=== Cliente de Subastas SASP ===");
        System.out.println("Conectando a " + host + ":" + PUERTO + "...");

        try {
            // Establecer la conexion TCP con el servidor
            socket = new Socket(host, PUERTO);
            System.out.println("Conexion establecida.\n");

            // Configurar flujos de entrada/salida sobre el socket
            salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Lanzar el hilo receptor de notificaciones en segundo plano
            receptor = new ReceptorNotificaciones(entrada);
            receptor.start();

            // Mostrar el menu y gestionar la interaccion del usuario
            menuPrincipal();

        } catch (ConnectException e) {
            System.err.println("[ERROR] No se pudo conectar al servidor en " + host + ":" + PUERTO);
            System.err.println("Comprueba que el servidor esta en marcha.");
        } catch (IOException e) {
            System.err.println("[ERROR] Problema de red: " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }

    // Muestra el menu de acciones disponibles y procesa la eleccion del usuario
    private void menuPrincipal() {
        // El primer paso obligatorio es registrarse
        solicitarRegistro();

        // Menu principal tras el registro
        while (activo) {
            mostrarMenu();
            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    crearSubasta();
                    break;
                case "2":
                    pujar();
                    break;
                case "3":
                    verSubasta();
                    break;
                case "4":
                    listarSubastas();
                    break;
                case "5":
                    desconectar();
                    break;
                default:
                    System.out.println("[!] Opcion no valida. Elige entre 1 y 5.");
                    break;
            }

            // Pausa breve para que las notificaciones asincronas se muestren antes del menu
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
    }

    // Solicita al usuario que se registre con un nombre unico
    private void solicitarRegistro() {
        boolean registrado = false;
        while (!registrado) {
            System.out.print("Introduce tu nombre de usuario: ");
            String nombre = scanner.nextLine().trim();

            if (nombre.isEmpty()) {
                System.out.println("[!] El nombre no puede estar vacio.");
                continue;
            }

            // Enviar mensaje de registro al servidor
            enviarMensaje(Mensaje.construir(Mensaje.REGISTRAR, nombre));

            // Esperar respuesta del servidor (el receptor la mostrara automaticamente)
            // Usamos una pequeña espera para que el receptor imprima la respuesta
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}

            // Preguntar si quiere continuar con ese nombre
            System.out.print("¿Registro correcto? Presiona ENTER para continuar o escribe 'no' para reintentar: ");
            String confirmacion = scanner.nextLine().trim();
            if (!confirmacion.equalsIgnoreCase("no")) {
                nombreUsuario = nombre;
                registrado = true;
            }
        }
    }

    // Muestra las opciones del menu
    private void mostrarMenu() {
        System.out.println("\n--- Menu principal [" + nombreUsuario + "] ---");
        System.out.println("1. Crear subasta");
        System.out.println("2. Pujar en una subasta");
        System.out.println("3. Ver estado de una subasta");
        System.out.println("4. Listar todas las subastas");
        System.out.println("5. Desconectarse");
        System.out.print("> ");
    }

    // Pide los datos de una nueva subasta y los envia al servidor
    private void crearSubasta() {
        System.out.println("\n-- Crear nueva subasta --");
        System.out.print("Nombre del objeto: ");
        String objeto = scanner.nextLine().trim();

        System.out.print("Precio base (ej: 100.0): ");
        String precio = scanner.nextLine().trim();

        System.out.print("Duracion en segundos (ej: 60): ");
        String duracion = scanner.nextLine().trim();

        // Enviar el mensaje al servidor
        enviarMensaje(Mensaje.construir(Mensaje.CREAR_SUBASTA, objeto, precio, duracion));
    }

    // Pide los datos de una puja y la envia al servidor
    private void pujar() {
        System.out.println("\n-- Pujar en una subasta --");
        System.out.print("ID de la subasta (ej: SUB-1): ");
        String idSubasta = scanner.nextLine().trim();

        System.out.print("Tu puja (ej: 150.0): ");
        String cantidad = scanner.nextLine().trim();

        enviarMensaje(Mensaje.construir(Mensaje.PUJAR, idSubasta, cantidad));
    }

    // Solicita al servidor el estado de una subasta concreta
    private void verSubasta() {
        System.out.println("\n-- Ver estado de subasta --");
        System.out.print("ID de la subasta (ej: SUB-1): ");
        String idSubasta = scanner.nextLine().trim();

        enviarMensaje(Mensaje.construir(Mensaje.VER_SUBASTA, idSubasta));
    }

    // Solicita al servidor el listado de todas las subastas
    private void listarSubastas() {
        enviarMensaje(Mensaje.construir(Mensaje.LISTAR_SUBASTAS));
    }

    // Envia la orden de desconexion al servidor y termina el cliente
    private void desconectar() {
        System.out.println("\nDesconectando...");
        enviarMensaje(Mensaje.construir(Mensaje.DESCONECTAR));
        activo = false;
    }

    // Envia un mensaje al servidor por el socket TCP
    private void enviarMensaje(String mensaje) {
        if (salida != null && !socket.isClosed()) {
            salida.print(mensaje);
            salida.flush();
        } else {
            System.err.println("[ERROR] No hay conexion con el servidor.");
            activo = false;
        }
    }

    // Cierra todos los recursos de red de forma ordenada
    private void cerrarConexion() {
        try {
            if (receptor != null) receptor.detener();
            if (salida   != null) salida.close();
            if (entrada  != null) entrada.close();
            if (socket   != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Al cerrar la conexion: " + e.getMessage());
        }
        System.out.println("Conexion cerrada. Adios.");
    }
}
