package P2.servidor;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import P2.modelo.InfoSubasta;
import P2.modelo.SubastaException;
import P2.remoto.ObservadorSubasta;

// Gestor central del estado del servidor.
public class GestorSubastasRMI {

    // Mapa usuario referencia para los callbacks
    private final Map<String, ObservadorSubasta> clientes = new ConcurrentHashMap<>();

    // Mapa de subastas activas
    private final Map<String, SubastaRMI> subastas = new ConcurrentHashMap<>();

    // Contador de IDs de subasta
    private final AtomicInteger contadorSubastas = new AtomicInteger(0);

    // ---------------------
    // Gestion de usuarios
    // ---------------------
    // Registra un usuario con su observador para callbacks. Lanza
    // SubastaException si el nombre ya esta en uso.
    public void registrarUsuario(String nombre, ObservadorSubasta observador)
            throws SubastaException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new SubastaException(SubastaException.PARAMETROS_INVALIDOS,
                    "El nombre de usuario no puede estar vacio");
        }
        if (clientes.containsKey(nombre)) {
            throw new SubastaException(SubastaException.USUARIO_DUPLICADO,
                    "El usuario '" + nombre + "' ya esta conectado");
        }
        clientes.put(nombre, observador);
        System.out.println("[GESTOR] Usuario registrado: " + nombre);
    }

    //Elimina un usuario al desconectarse.
    public void desconectarUsuario(String nombre) {
        if (nombre != null) {
            clientes.remove(nombre);
            System.out.println("[GESTOR] Usuario desconectado: " + nombre);
        }
    }

    //Comprueba si un usuario esta registrado.
    public boolean existeUsuario(String nombre) {
        return clientes.containsKey(nombre);
    }

    // --------------------
    // Gestion de subastas
    // --------------------
    // Crea una nueva subasta y la registra. Devuelve su ID.
    public String crearSubasta(String objeto, double precioBase, int duracionSegundos) {
        String id = "SUB-" + contadorSubastas.incrementAndGet();
        SubastaRMI nueva = new SubastaRMI(id, objeto, precioBase, duracionSegundos, this);
        subastas.put(id, nueva);
        System.out.println("[GESTOR] Subasta creada: " + id
                + " objeto=" + objeto
                + " precioBase=" + precioBase
                + " duracion=" + duracionSegundos + "s");
        return id;
    }

    //Devuelve la subasta o lanza SubastaException si no existe.
    public SubastaRMI obtenerSubasta(String id) throws SubastaException {
        SubastaRMI s = subastas.get(id);
        if (s == null) {
            throw new SubastaException(SubastaException.SUBASTA_INEXISTENTE,
                    "No existe ninguna subasta con ID: " + id);
        }
        return s;
    }

    //Devuelve la lista de InfoSubasta de todas las subastas.
    public List<InfoSubasta> listarSubastas() {
        List<InfoSubasta> lista = new ArrayList<>();
        for (SubastaRMI s : subastas.values()) {
            lista.add(s.toInfo());
        }
        return lista;
    }

    // --------------------------------
    // Notificaciones via callback RMI
    // --------------------------------
    // Llama a onNuevaPuja() en TODOS los clientes conectados.
    // Si un cliente falla (RemoteException),se elimina de la lista.
    public void notificarNuevaPuja(SubastaRMI subasta, boolean tiempoExtendido) {
        InfoSubasta info = subasta.toInfo();
        List<String> clientesCaidos = new ArrayList<>();

        for (Map.Entry<String, ObservadorSubasta> entrada : clientes.entrySet()) {
            try {
                entrada.getValue().onNuevaPuja(info, tiempoExtendido);
            } catch (RemoteException e) {
                System.err.println("[GESTOR] Cliente caido (puja): "
                        + entrada.getKey() + " -> " + e.getMessage());
                clientesCaidos.add(entrada.getKey());
            }
        }
        clientesCaidos.forEach(clientes::remove);
    }

    // Llama a onSubastaCerrada() en TODOS los clientes conectados.
    public void notificarSubastaCerrada(SubastaRMI subasta) {
        InfoSubasta info = subasta.toInfo();
        String ganador = info.getMejorPostor() != null ? info.getMejorPostor() : "Nadie";
        System.out.println("[GESTOR] Subasta cerrada: " + subasta.getId()
                + " | Ganador: " + ganador
                + " | Precio: " + info.getPrecioActual());

        List<String> clientesCaidos = new ArrayList<>();

        for (Map.Entry<String, ObservadorSubasta> entrada : clientes.entrySet()) {
            try {
                entrada.getValue().onSubastaCerrada(info);
            } catch (RemoteException e) {
                System.err.println("[GESTOR] Cliente caido (cierre): "
                        + entrada.getKey() + " -> " + e.getMessage());
                clientesCaidos.add(entrada.getKey());
            }
        }
        clientesCaidos.forEach(clientes::remove);
    }
}
