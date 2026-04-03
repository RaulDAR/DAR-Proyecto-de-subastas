package cliente;

import comun.Mensaje;
import java.io.BufferedReader;
import java.io.IOException;

// Hilo receptor del cliente: escucha en segundo plano los mensajes que el servidor
// envia de forma asincrona (notificaciones de nuevas pujas, cierres de subasta, etc.)
// Corre en paralelo al hilo principal del cliente que muestra el menu.
public class ReceptorNotificaciones extends Thread {

    // Flujo de entrada desde el servidor
    private final BufferedReader entrada;

    // Bandera para detener el hilo de forma controlada
    private volatile boolean activo = true;

    // Constructor
    public ReceptorNotificaciones(BufferedReader entrada) {
        this.entrada = entrada;
        setName("receptor-notificaciones");
        // Hilo daemon: si el programa principal termina, este tambien termina
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            String lineaRecibida;
            // Bucle: esperar y procesar mensajes del servidor
            while (activo && (lineaRecibida = entrada.readLine()) != null) {
                String linea = lineaRecibida.trim();
                if (!linea.isEmpty()) {
                    procesarMensajeServidor(linea);
                }
            }
        } catch (IOException e) {
            if (activo) {
                // Solo mostrar error si no fue una desconexion voluntaria
                System.err.println("\n[!] Se ha perdido la conexion con el servidor: " + e.getMessage());
            }
        }
    }

    // Interpreta el mensaje recibido del servidor y lo muestra al usuario
    private void procesarMensajeServidor(String mensajeRaw) {
        String[] campos = Mensaje.dividir(mensajeRaw);
        if (campos.length == 0) return;

        String tipo = campos[0];

        switch (tipo) {
            case Mensaje.OK:
                // Respuesta positiva a una operacion del cliente
                // Se imprime directamente, el hilo principal espera su respuesta
                if (campos.length >= 2) {
                    System.out.println("\n[OK] " + campos[1]);
                }
                break;

            case Mensaje.ERROR:
                // Respuesta de error del servidor
                if (campos.length >= 3) {
                    System.out.println("\n[ERROR " + campos[1] + "] " + campos[2]);
                } else if (campos.length == 2) {
                    System.out.println("\n[ERROR] " + campos[1]);
                }
                break;

            case Mensaje.NOTIFICACION:
                // Notificacion asincrona: procesarla segun su subtipo
                if (campos.length >= 2) {
                    procesarNotificacion(campos);
                }
                break;

            default:
                // Mensaje desconocido: mostrar en bruto para depuracion
                System.out.println("\n[SERVIDOR] " + mensajeRaw);
                break;
        }

        // Volver a mostrar el prompt despues de una notificacion
        System.out.print("> ");
    }

    // Muestra en pantalla la notificacion del servidor de forma legible
    private void procesarNotificacion(String[] campos) {
        // campos[0] = NOTIFICACION, campos[1] = tipo
        String tipoNotif = campos[1];

        switch (tipoNotif) {
            case Mensaje.NOTIF_NUEVA_PUJA:
                // NOTIFICACION|NUEVA_PUJA|idSubasta|objeto|precioActual|mejorPostor|tiempoRestante
                if (campos.length >= 7) {
                    System.out.println("\n*** NUEVA PUJA ***");
                    System.out.println("  Subasta : " + campos[2] + " (" + campos[3] + ")");
                    System.out.println("  Precio  : " + campos[4]);
                    System.out.println("  Postor  : " + campos[5]);
                    System.out.println("  Tiempo  : " + campos[6] + "s restantes");
                }
                break;

            case Mensaje.NOTIF_TIEMPO_EXT:
                // NOTIFICACION|TIEMPO_EXTENDIDO|idSubasta|nuevoTiempoRestante
                if (campos.length >= 4) {
                    System.out.println("\n*** TIEMPO EXTENDIDO ***");
                    System.out.println("  Subasta : " + campos[2]);
                    System.out.println("  Nuevo tiempo restante: " + campos[3] + "s");
                }
                break;

            case Mensaje.NOTIF_SUBASTA_CERRADA:
                // NOTIFICACION|SUBASTA_CERRADA|idSubasta|objeto|ganador|precioFinal|numPujas
                if (campos.length >= 7) {
                    System.out.println("\n========================================");
                    System.out.println("  SUBASTA CERRADA: " + campos[2]);
                    System.out.println("  Objeto     : " + campos[3]);
                    System.out.println("  Ganador    : " + campos[4]);
                    System.out.println("  Precio final: " + campos[5]);
                    System.out.println("  Total pujas: " + campos[6]);
                    System.out.println("========================================");
                }
                break;

            default:
                System.out.println("\n[NOTIF] " + tipoNotif + ": " + String.join(" | ", campos));
                break;
        }
    }

    // Detiene el receptor de forma controlada
    public void detener() {
        activo = false;
        interrupt();
    }
}
