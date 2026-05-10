package P2.servidor;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import P2.remoto.SubastaServicioRemoto;

// Punto de entrada del servidor de subastas RMI.
// El propio RMI gestiona los hilos por cliente internamente.
public class ServidorRMI {

    public static final String NOMBRE_SERVICIO = "SubastaServicio";
    public static final int PUERTO_DEFECTO = 1099;  // puerto estandar RMI

    public static void main(String[] args) {
        System.setProperty("java.rmi.server.hostname", "192.168.1.187");

        int puerto = PUERTO_DEFECTO;
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("[!] Puerto invalido, usando " + PUERTO_DEFECTO);
            }
        }

        String hostname = System.getProperty("java.rmi.server.hostname");
        if (hostname != null) {
            System.out.println("[SERVIDOR] RMI hostname fijada a: " + hostname);
        }

        System.out.println("=== Servidor de Subastas RMI ===");
        System.out.println("Puerto RMI registry: " + puerto);

        try {
            // 1. Crear el registry local en el puerto indicado
            Registry registry = LocateRegistry.createRegistry(puerto);

            // 2. Instanciar el servicio 
            SubastaServicioRemoto servicio = new SubastaServicioImpl();

            // 3. Registrarlo en el registry con el nombre conocido
            registry.rebind(NOMBRE_SERVICIO, servicio);

            System.out.println("Servicio '" + NOMBRE_SERVICIO + "' registrado. Esperando clientes...\n");

            // El servidor queda bloqueado aqui 
            synchronized (ServidorRMI.class) {
                ServidorRMI.class.wait();
            }

        } catch (Exception e) {
            System.err.println("[SERVIDOR] Error fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
