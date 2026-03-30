package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// Clase principal del servidor de subastas.
// Escucha en un puerto TCP, acepta conexiones y lanza un hilo por cada cliente.
public class ServidorSubasta {

    // Puerto en el que escucha el servidor
    private static final int PUERTO = 9090;

    public static void main(String[] args) {
        System.out.println("=== Servidor de Subastas SASP ===");
        System.out.println("Puerto: " + PUERTO);
        System.out.println("Esperando conexiones...\n");

        // El gestor mantiene el estado global: subastas y usuarios
        GestorSubastas gestor = new GestorSubastas();

        // Abrir el ServerSocket en el puerto definido
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {

            // Bucle principal del servidor: acepta conexiones indefinidamente
            while (true) {
                try {
                    // Bloquea hasta que un cliente se conecta
                    Socket socketCliente = serverSocket.accept();

                    // Lanzar un hilo dedicado para este cliente
                    ManejadorCliente manejador = new ManejadorCliente(socketCliente, gestor);
                    manejador.start();

                } catch (IOException e) {
                    // Error al aceptar una conexion concreta: continuar con el resto
                    System.err.println("[SERVIDOR] Error al aceptar conexion: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            // Error critico: no se pudo abrir el puerto
            System.err.println("[SERVIDOR] Error fatal al iniciar el servidor en puerto "
                    + PUERTO + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
