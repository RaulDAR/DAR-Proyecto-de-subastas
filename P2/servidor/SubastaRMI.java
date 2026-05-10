package P2.servidor;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import P2.modelo.InfoSubasta;
import P2.modelo.SubastaException;

// Clase que representa el estado completo de una subasta en el servidor.
public class SubastaRMI {

    private final String id;
    private final String objeto;
    private final double precioBase;
    private double precioActual;
    private String mejorPostor;
    private final int duracionSegundos;
    private int tiempoRestante;
    private boolean abierta;
    private final List<String> historicoPujas;
    private final GestorSubastasRMI gestor;
    private Timer temporizador;

    // Mismos parametros de extension que en la practica 1
    public static final int SEGUNDOS_EXTENSION = 10;
    public static final int VENTANA_FINAL = 15;

    public SubastaRMI(String id, String objeto, double precioBase,
            int duracionSegundos, GestorSubastasRMI gestor) {
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

        iniciarTemporizador();
    }

    private void iniciarTemporizador() {
        temporizador = new Timer("timer-subasta-" + id, true);
        temporizador.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (SubastaRMI.this) {
                    if (!abierta) {
                        cancel();
                        return;
                    }
                    tiempoRestante--;
                    if (tiempoRestante <= 0) {
                        cerrarSubasta();
                        cancel();
                    }
                }
            }
        }, 1000, 1000);
    }

    // Intenta registrar una puja.
    // Lanza SubastaException si no es valida (mismo comportamiento que en P1,
    // pero ahora la excepcion viaja por RMI hasta el cliente automaticamente).
    public synchronized void intentarPujar(String usuario, double cantidad)
            throws SubastaException {

        if (!abierta) {
            throw new SubastaException(SubastaException.SUBASTA_CERRADA, "La subasta " + id + " ya esta cerrada");
        }
        if (usuario.equals(mejorPostor)) {
            throw new SubastaException(SubastaException.AUTOPUJA, "Ya eres el mejor postor. Espera a que otro usuario puje");
        }
        if (cantidad <= precioActual) {
            throw new SubastaException(SubastaException.PUJA_INSUFICIENTE,
                    "La puja minima es " + String.format("%.2f", precioActual + 0.01) + ". Tu puja: " + cantidad);
        }

        precioActual = cantidad;
        mejorPostor = usuario;
        historicoPujas.add(usuario + ":" + cantidad);

        boolean extendido = false;
        if (tiempoRestante <= VENTANA_FINAL) {
            tiempoRestante += SEGUNDOS_EXTENSION;
            extendido = true;
        }

        gestor.notificarNuevaPuja(this, extendido);
    }

    private void cerrarSubasta() {
        abierta = false;
        gestor.notificarSubastaCerrada(this);
    }

    //Genera el DTO serializable para enviar al cliente via RMI
    public synchronized InfoSubasta toInfo() {
        return new InfoSubasta(id, objeto, precioBase, precioActual,
                mejorPostor, abierta, tiempoRestante, historicoPujas.size());
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getObjeto() {
        return objeto;
    }

    public synchronized double getPrecioActual() {
        return precioActual;
    }

    public synchronized String getMejorPostor() {
        return mejorPostor;
    }

    public synchronized boolean isAbierta() {
        return abierta;
    }

    public synchronized int getTiempoRestante() {
        return tiempoRestante;
    }

    public synchronized List<String> getHistoricoPujas() {
        return new ArrayList<>(historicoPujas);
    }
}
