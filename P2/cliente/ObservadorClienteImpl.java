package P2.cliente;

import P2.modelo.InfoSubasta;
import P2.remoto.ObservadorSubasta;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


// Implementacion del observador en el lado del cliente.
// El cliente exporta este objeto como objeto remoto para que el servidor
// pueda llamar a sus metodos cuando ocurran eventos (nueva puja, cierre).


public class ObservadorClienteImpl extends UnicastRemoteObject implements ObservadorSubasta {

    private static final long serialVersionUID = 1L;

    public ObservadorClienteImpl() throws RemoteException {
        super();
    }

    @Override
    public void onNuevaPuja(InfoSubasta subasta, boolean tiempoExtendido)
            throws RemoteException {
        System.out.println("\n*** NUEVA PUJA ***");
        System.out.println("  Subasta : " + subasta.getId() + " (" + subasta.getObjeto() + ")");
        System.out.println("  Precio  : " + subasta.getPrecioActual());
        System.out.println("  Postor  : " + subasta.getMejorPostor());
        System.out.println("  Tiempo  : " + subasta.getTiempoRestante() + "s restantes");
        if (tiempoExtendido) {
            System.out.println("  [!] Tiempo extendido - nuevo tiempo: " + subasta.getTiempoRestante() + "s");
        }
        System.out.print("> ");
    }

    @Override
    public void onSubastaCerrada(InfoSubasta subasta) throws RemoteException {
        String ganador = subasta.getMejorPostor() != null
                ? subasta.getMejorPostor() : "Nadie";
        System.out.println("\n========================================");
        System.out.println("  SUBASTA CERRADA: " + subasta.getId());
        System.out.println("  Objeto     : " + subasta.getObjeto());
        System.out.println("  Ganador    : " + ganador);
        System.out.println("  Precio final: " + subasta.getPrecioActual());
        System.out.println("  Total pujas: " + subasta.getNumeroPujas());
        System.out.println("========================================");
        System.out.print("> ");
    }
}
