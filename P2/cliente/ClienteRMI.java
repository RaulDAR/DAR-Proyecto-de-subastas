package P2.cliente;

import P2.modelo.InfoSubasta;
import P2.modelo.SubastaException;
import P2.remoto.ObservadorSubasta;
import P2.remoto.SubastaServicioRemoto;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;


// java -cp out subasta.cliente.ClienteRMI [host] [puerto]


public class ClienteRMI {

    private static final String HOST_DEFECTO  = "localhost";
    private static final int PUERTO_DEFECTO = 1099;

    // Referencia al servicio remoto (el "stub" generado por RMI)
    private SubastaServicioRemoto servicio;

    // Nombre del usuario registrado en esta sesion
    private String nombreUsuario = null;

    private final Scanner scanner = new Scanner(System.in);
    private boolean activo = true;

    public static void main(String[] args) {
        String host  = (args.length > 0) ? args[0] : HOST_DEFECTO;
        int puerto = PUERTO_DEFECTO;
        if (args.length > 1) {
            try { puerto = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) {
                System.err.println("[!] Puerto invalido, usando " + PUERTO_DEFECTO);
            }
        }

        new ClienteRMI().iniciar(host, puerto);
    }

    public void iniciar(String host, int puerto) {
        System.out.println("=== Cliente de Subastas RMI ===");
        System.out.println("Conectando al registry en " + host + ":" + puerto + "...");

        try {
            // 1. Localizar el registry remoto
            Registry registry = LocateRegistry.getRegistry(host, puerto);

            // 2. Obtener el stub del servicio remoto
            servicio = (SubastaServicioRemoto) registry.lookup("SubastaServicio");
            System.out.println("Conectado al servicio de subastas.\n");

            // 3. Crear y exportar el observador para recibir notificaciones asincronas
            ObservadorSubasta observador = new ObservadorClienteImpl();

            // 4. Registrarse en el servidor
            solicitarRegistro(observador);

            // 5. Bucle de menu
            while (activo) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();

                switch (opcion) {
                    case "1": crearSubasta(); break;
                    case "2": pujar(); break;
                    case "3": verSubasta(); break;
                    case "4": listarSubastas(); break;
                    case "5": desconectar(); break;
                    default:
                        System.out.println("[!] Opcion no valida. Elige entre 1 y 5.");
                }

                // Pausa breve para que los callbacks se impriman antes del menu
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }

        } catch (NotBoundException e) {
            System.err.println("[ERROR] El servicio 'SubastaServicio' no esta registrado en el registry.");
            System.err.println("Comprueba que el servidor esta en marcha.");
        } catch (RemoteException e) {
            System.err.println("[ERROR] No se pudo conectar al servidor RMI en " + host + ":" + puerto);
            System.err.println("Causa: " + e.getMessage());
            System.err.println("Comprueba que el servidor esta en marcha.");
        } finally {
            System.out.println("Adios.");
        }
    }

    // -------------------------------------------------------------------------
    // Operaciones del menu (equivalentes a los metodos del ClienteSubasta P1)
    // -------------------------------------------------------------------------

    private void solicitarRegistro(ObservadorSubasta observador) {
        boolean registrado = false;
        while (!registrado) {
            System.out.print("Introduce tu nombre de usuario: ");
            String nombre = scanner.nextLine().trim();

            if (nombre.isEmpty()) {
                System.out.println("[!] El nombre no puede estar vacio.");
                continue;
            }

            try {
                // Llamada directa al metodo remoto (sin construir ningun mensaje de texto)
                servicio.registrarUsuario(nombre, observador);
                nombreUsuario = nombre;
                registrado = true;
                System.out.println("[OK] Bienvenido, " + nombre + ". Estas registrado en el sistema de subastas.");

            } catch (SubastaException e) {
                // El servidor lanzo un error de dominio (ej: usuario duplicado)
                System.out.println("[ERROR " + e.getCodigo() + "] " + e.getMessage());
            } catch (RemoteException e) {
                System.err.println("[ERROR] Fallo de comunicacion: " + e.getMessage());
                activo = false;
                return;
            }
        }
    }

    private void mostrarMenu() {
        System.out.println("\n--- Menu principal [" + nombreUsuario + "] ---");
        System.out.println("1. Crear subasta");
        System.out.println("2. Pujar en una subasta");
        System.out.println("3. Ver estado de una subasta");
        System.out.println("4. Listar todas las subastas");
        System.out.println("5. Desconectarse");
        System.out.print("> ");
    }

    private void crearSubasta() {
        System.out.println("\n-- Crear nueva subasta --");
        System.out.print("Nombre del objeto: ");
        String objeto = scanner.nextLine().trim();

        System.out.print("Precio base (ej: 100.0): ");
        double precioBase;
        try { precioBase = Double.parseDouble(scanner.nextLine().trim()); }
        catch (NumberFormatException e) {
            System.out.println("[!] Precio invalido."); return;
        }

        System.out.print("Duracion en segundos (ej: 60): ");
        int duracion;
        try { duracion = Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) {
            System.out.println("[!] Duracion invalida."); return;
        }

        try {
            String id = servicio.crearSubasta(nombreUsuario, objeto, precioBase, duracion);
            System.out.println("[OK] Subasta creada. ID: " + id + " | Objeto: " + objeto + " | Precio base: " + precioBase + " | Duracion: " + duracion + "s");

        } catch (SubastaException e) {
            System.out.println("[ERROR " + e.getCodigo() + "] " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("[ERROR] Fallo de red: " + e.getMessage());
        }
    }

    private void pujar() {
        System.out.println("\n-- Pujar en una subasta --");
        System.out.print("ID de la subasta (ej: SUB-1): ");
        String idSubasta = scanner.nextLine().trim();

        System.out.print("Tu puja (ej: 150.0): ");
        double cantidad;
        try { cantidad = Double.parseDouble(scanner.nextLine().trim()); }
        catch (NumberFormatException e) {
            System.out.println("[!] Cantidad invalida."); return;
        }

        try {
            servicio.pujar(nombreUsuario, idSubasta, cantidad);
            System.out.println("[OK] Puja de " + cantidad + " aceptada en subasta " + idSubasta + ". Eres el mejor postor.");

        } catch (SubastaException e) {
            System.out.println("[ERROR " + e.getCodigo() + "] " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("[ERROR] Fallo de red: " + e.getMessage());
        }
    }

    private void verSubasta() {
        System.out.println("\n-- Ver estado de subasta --");
        System.out.print("ID de la subasta (ej: SUB-1): ");
        String idSubasta = scanner.nextLine().trim();

        try {
            InfoSubasta info = servicio.verSubasta(idSubasta);
            System.out.println("\n" + info);

        } catch (SubastaException e) {
            System.out.println("[ERROR " + e.getCodigo() + "] " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("[ERROR] Fallo de red: " + e.getMessage());
        }
    }

    private void listarSubastas() {
        try {
            List<InfoSubasta> lista = servicio.listarSubastas();

            if (lista.isEmpty()) {
                System.out.println("[OK] No hay subastas disponibles en este momento.");
                return;
            }

            System.out.println("\n=== Lista de subastas (" + lista.size() + ") ===");
            for (InfoSubasta s : lista) {
                System.out.print("[" + s.getId() + "] " + s.getObjeto() + " | Precio actual: " + s.getPrecioActual() + " | ");
                if (s.isAbierta()) {
                    System.out.println("ABIERTA - " + s.getTiempoRestante() + "s restantes");
                } else {
                    String ganador = s.getMejorPostor() != null ? s.getMejorPostor() : "Nadie";
                    System.out.println("CERRADA - Ganador: " + ganador);
                }
            }

        } catch (RemoteException e) {
            System.err.println("[ERROR] Fallo de red: " + e.getMessage());
        }
    }

    private void desconectar() {
        System.out.println("\nDesconectando...");
        try {
            servicio.desconectar(nombreUsuario);
        } catch (RemoteException e) {
            // Si ya no hay red, simplemente salimos
            System.err.println("[!] Error al notificar desconexion: " + e.getMessage());
        }
        activo = false;
    }
}
