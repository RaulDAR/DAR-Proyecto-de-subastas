package subasta.remoto;

import subasta.modelo.InfoSubasta;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ObservadorSubasta extends Remote {

    /**
     * El servidor notifica que se ha registrado una nueva puja.
     * 
     * @param subasta         estado actualizado de la subasta tras la puja
     * @param tiempoExtendido true si el tiempo se ha extendido por ventana final
     * @throws RemoteException si hay fallo de comunicacion
     */
    void onNuevaPuja(InfoSubasta subasta, boolean tiempoExtendido) throws RemoteException;

    /**
     * El servidor notifica que una subasta ha cerrado.
     * 
     * @param subasta estado final de la subasta (cerrada, con ganador)
     * @throws RemoteException si hay fallo de comunicacion
     */
    void onSubastaCerrada(InfoSubasta subasta) throws RemoteException;
}
  
