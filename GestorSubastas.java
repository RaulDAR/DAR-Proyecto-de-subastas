package servidor;

import comun.Mensaje;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Clase central del servidor: gestiona el estado global de todas las subastas
// y de todos los clientes conectados.
// Uso de ConcurrentHashMap para garantizar consistencia bajo concurrencia.
public class GestorSubastas {

    // Mapa de subastas activas: idSubasta -> objeto Subasta
    private final Map<String, Subasta> subastas = new ConcurrentHashMap<>();

    // Mapa de clientes conectados: nombreUsuario -> su PrintWriter para notificaciones
    // Un cliente puede estar registrado sin estar suscrito a ninguna subasta concreta;
    // las notificaciones se envian a todos los clientes conectados
    private final Map<String, PrintWriter> clientesConectados = new ConcurrentHashMap<>();

    // Contador para generar IDs de subasta unicos (SUB-1, SUB-2, ...)
    private final AtomicInteger contadorSubastas = new AtomicInteger(0);

    // Registra un nuevo usuario. Devuelve null si OK, o mensaje de error si el nombre ya existe
    public String registrarUsuario(String nombre, PrintWriter salida) {
        if (clientesConectados.containsKey(nombre)) {
            return Mensaje.error(Mensaje.ERR_USUARIO_DUPLICADO,
                    "El usuario '" + nombre + "' ya esta conectado");
        }
        clientesConectados.put(nombre, salida);
        System.out.println("[GESTOR] Usuario registrado: " + nombre);
        return null;
    }

    // Elimina un usuario cuando se desconecta
    public void desconectarUsuario(String nombre) {
        if (nombre != null) {
            clientesConectados.remove(nombre);
            System.out.println("[GESTOR] Usuario desconectado: " + nombre);
        }
    }

    // Comprueba si un nombre de usuario ya esta registrado
    public boolean existeUsuario(String nombre) {
        return clientesConectados.containsKey(nombre);
    }

    // Crea una nueva subasta y la registra. Devuelve el ID generado, o null si hay error
    // Se devuelve el error como String si los parametros son invalidos
    public String crearSubasta(String objeto, double precioBase, int duracionSegundos) {
        String id = "SUB-" + contadorSubastas.incrementAndGet();
        Subasta nueva = new Subasta(id, objeto, precioBase, duracionSegundos, this);
        subastas.put(id, nueva);
        System.out.println("[GESTOR] Subasta creada: " + id + " objeto=" + objeto
                + " precioBase=" + precioBase + " duracion=" + duracionSegundos + "s");
        return id;
    }

    // Devuelve la subasta con ese ID, o null si no existe
    public Subasta obtenerSubasta(String id) {
        return subastas.get(id);
    }

    // Devuelve un listado de todas las subastas (abiertas y cerradas)
    public List<Subasta> listarSubastas() {
        return new ArrayList<>(subastas.values());
    }

    // Envia una notificacion de nueva puja a todos los clientes conectados
    // Llamado desde el hilo del temporizador de Subasta, sincronizado externamente
    public void notificarNuevaPuja(Subasta subasta, boolean tiempoExtendido) {
        // Construir la notificacion de nueva puja
        String msgPuja = Mensaje.notificacion(
                Mensaje.NOTIF_NUEVA_PUJA,
                subasta.getId(),
                subasta.getObjeto(),
                String.valueOf(subasta.getPrecioActual()),
                subasta.getMejorPostor(),
                String.valueOf(subasta.getTiempoRestante())
        );

        enviarATodos(msgPuja);

        // Si el tiempo se ha extendido, enviar tambien esa notificacion
        if (tiempoExtendido) {
            String msgExt = Mensaje.notificacion(
                    Mensaje.NOTIF_TIEMPO_EXT,
                    subasta.getId(),
                    String.valueOf(subasta.getTiempoRestante())
            );
            enviarATodos(msgExt);
        }
    }

    // Envia la notificacion de cierre de subasta a todos los clientes
    public void notificarSubastaCerrada(Subasta subasta) {
        String ganador = subasta.getMejorPostor() != null ? subasta.getMejorPostor() : "Nadie";
        String precio  = String.valueOf(subasta.getPrecioActual());

        String msg = Mensaje.notificacion(
                Mensaje.NOTIF_SUBASTA_CERRADA,
                subasta.getId(),
                subasta.getObjeto(),
                ganador,
                precio,
                String.valueOf(subasta.getHistoricoPujas().size())
        );

        System.out.println("[GESTOR] Subasta cerrada: " + subasta.getId()
                + " | Ganador: " + ganador + " | Precio: " + precio);

        enviarATodos(msg);
    }

    // Envia un mensaje a todos los clientes registrados actualmente
    private void enviarATodos(String mensaje) {
        for (Map.Entry<String, PrintWriter> entrada : clientesConectados.entrySet()) {
            try {
                PrintWriter pw = entrada.getValue();
                // PrintWriter es hilo-seguro para println si el socket subyacente lo es
                pw.print(mensaje);
                pw.flush();
            } catch (Exception e) {
                // Si un cliente esta caido, continuar con el resto
                System.err.println("[GESTOR] Error al notificar a " + entrada.getKey() + ": " + e.getMessage());
            }
        }
    }
}
