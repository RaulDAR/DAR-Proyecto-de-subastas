package subasta.modelo;

import java.io.Serializable;

// DTO (Data Transfer Object) que representa el estado de una subasta.
// Es Serializable para poder enviarse entre JVMs via RMI.
// Sustituye al formateo manual de cadenas del protocolo SASP.

public class InfoSubasta implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String objeto;
    private final double precioBase;
    private final double precioActual;
    private final String mejorPostor; // null si nadie ha pujado
    private final boolean abierta;
    private final int tiempoRestante;
    private final int numeroPujas;

    public InfoSubasta(String id, String objeto, double precioBase, double precioActual, String mejorPostor, boolean abierta, int tiempoRestante, int numeroPujas) {
        this.id = id;
        this.objeto = objeto;
        this.precioBase = precioBase;
        this.precioActual = precioActual;
        this.mejorPostor = mejorPostor;
        this.abierta = abierta;
        this.tiempoRestante = tiempoRestante;
        this.numeroPujas = numeroPujas;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getObjeto() {
        return objeto;
    }

    public double getPrecioBase() {
        return precioBase;
    }

    public double getPrecioActual() {
        return precioActual;
    }

    public String getMejorPostor() {
        return mejorPostor;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public int getTiempoRestante() {
        return tiempoRestante;
    }

    public int getNumeroPujas() {
        return numeroPujas;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Subasta ").append(id).append(" ===\n");
        sb.append("Objeto       : ").append(objeto).append("\n");
        sb.append("Precio base  : ").append(precioBase).append("\n");
        sb.append("Precio actual: ").append(precioActual).append("\n");
        sb.append("Mejor postor : ").append(mejorPostor != null ? mejorPostor : "Ninguno").append("\n");
        sb.append("Estado       : ").append(abierta ? "ABIERTA" : "CERRADA").append("\n");
        sb.append("Tiempo rest. : ").append(tiempoRestante).append(" segundos\n");
        sb.append("Pujas totales: ").append(numeroPujas);
        return sb.toString();
    }
}
