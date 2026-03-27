package servidor;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

// Clase que representa el estado completo de una subasta en el servidor
// El servidor es la unica entidad que mantiene este estado
public class Subasta {

    // Identificador unico de la subasta (asignado por el servidor)
    private final String id;

    // Descripcion del objeto subastado
    private final String objeto;

    // Precio de salida definido por el creador
    private final double precioBase;

    // Precio actual (la mejor puja hasta el momento)
    private double precioActual;

    // Nombre del mejor postor actual (null si nadie ha pujado)
    private String mejorPostor;

    // Duracion inicial en segundos
    private final int duracionSegundos;

    // Tiempo restante en segundos (se actualiza con el temporizador)
    private int tiempoRestante;

    // Estado de la subasta: true = abierta, false = cerrada
    private boolean abierta;

    // Historico de pujas: cada entrada es "usuario:cantidad"
    private final List<String> historicoPujas;

    // Referencia al gestor para lanzar notificaciones cuando cambia el estado
    private final GestorSubastas gestor;

    // Temporizador interno que cuenta el tiempo de la subasta
    private Timer temporizador;

    // Segundos de extension cuando entra una puja en la ventana final
    public static final int SEGUNDOS_EXTENSION = 10;

    // Ventana final: si queda menos de X segundos y entra puja, se extiende
    public static final int VENTANA_FINAL = 15;

    // Constructor
    public Subasta(String id, String objeto, double precioBase, int duracionSegundos, GestorSubastas gestor) {
        this.id = id;
        this.objeto = objeto;
        this.precioBase = precioBase;
        this.precioActual = precioBase;
        this.mejorPostor = null;
        this.duracionSegundos = duracionSegundos;
        this.tiempoRestante = duracionSegundos;
        this.abierta = true;
        this.historicoPujas = new ArrayList<>();
        this.gestor = gestor;

        // Iniciar el temporizador interno de la subasta
        iniciarTemporizador();
    }

    // Inicia el temporizador que descuenta el tiempo cada segundo
    private void iniciarTemporizador() {
        temporizador = new Timer("temporizador-subasta-" + id, true);
        temporizador.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (Subasta.this) {
                    if (!abierta) {
                        cancel();
                        return;
                    }
                    tiempoRestante--;
                    if (tiempoRestante <= 0) {
                        // El tiempo ha expirado: cerrar la subasta
                        cerrarSubasta();
                        cancel();
                    }
                }
            }
        }, 1000, 1000); // empieza tras 1s, repite cada 1s
    }

    // Intenta registrar una puja de un usuario por la cantidad indicada
    // Devuelve null si la puja es valida, o el mensaje de error si no lo es
    public synchronized String intentarPujar(String usuario, double cantidad) {
        // La subasta ya cerro
        if (!abierta) {
            return comun.Mensaje.error(comun.Mensaje.ERR_SUBASTA_CERRADA,
                    "La subasta " + id + " ya esta cerrada");
        }

        // El mejor postor actual no puede volver a pujar
        if (usuario.equals(mejorPostor)) {
            return comun.Mensaje.error(comun.Mensaje.ERR_AUTOPUJA,
                    "Ya eres el mejor postor. Espera a que otro usuario puje");
        }

        // La cantidad debe superar el precio actual
        if (cantidad <= precioActual) {
            return comun.Mensaje.error(comun.Mensaje.ERR_PUJA_INSUFICIENTE,
                    "La puja minima es " + (precioActual + 0.01) + ". Tu puja: " + cantidad);
        }

        // Puja valida: actualizar estado
        precioActual = cantidad;
        mejorPostor = usuario;
        historicoPujas.add(usuario + ":" + cantidad);

        // Comprobar si estamos en la ventana final para extender el tiempo
        boolean extendido = false;
        if (tiempoRestante <= VENTANA_FINAL) {
            tiempoRestante += SEGUNDOS_EXTENSION;
            extendido = true;
        }

        // Notificar a todos los participantes sobre la nueva puja
        gestor.notificarNuevaPuja(this, extendido);

        return null; // null significa que la puja fue aceptada
    }

    // Cierra la subasta y notifica a todos los participantes
    private void cerrarSubasta() {
        abierta = false;
        gestor.notificarSubastaCerrada(this);
    }

    // Devuelve una descripcion del estado actual en formato legible
    public synchronized String obtenerEstado() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Subasta ").append(id).append(" ===\n");
        sb.append("Objeto: ").append(objeto).append("\n");
        sb.append("Precio base: ").append(precioBase).append("\n");
        sb.append("Precio actual: ").append(precioActual).append("\n");
        sb.append("Mejor postor: ").append(mejorPostor != null ? mejorPostor : "Ninguno").append("\n");
        sb.append("Estado: ").append(abierta ? "Abierta" : "Cerrada").append("\n");
        sb.append("Tiempo restante: ").append(tiempoRestante).append(" segundos\n");
        sb.append("Pujas registradas: ").append(historicoPujas.size());
        return sb.toString();
    }

    // Getters
    public String getId() { return id; }
    public String getObjeto() { return objeto; }
    public double getPrecioActual() { return precioActual; }
    public String getMejorPostor() { return mejorPostor; }
    public boolean isAbierta() { return abierta; }
    public int getTiempoRestante() { return tiempoRestante; }
    public List<String> getHistoricoPujas() { return new ArrayList<>(historicoPujas); }
}
