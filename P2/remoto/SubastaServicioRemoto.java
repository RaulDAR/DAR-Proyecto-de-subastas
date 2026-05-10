package P2.remoto;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import P2.modelo.InfoSubasta;
import P2.modelo.SubastaException;

// Interfaz remota del servicio de subastas (Java RMI).
// Cada metodo debe declarar RemoteException (fallo de red/RMI).
// Los errores de dominio se propagan como SubastaException.
// Las notificaciones asincronas (NOTIF_NUEVA_PUJA, NOTIF_SUBASTA_CERRADA...
// se implementan mediante callback: el cliente expone su propia interfaz remota
//(ObservadorSubasta) y la registra en el servidor al conectarse.
public interface SubastaServicioRemoto extends Remote {

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param nombreUsuario nombre unico del usuario
     * @throws SubastaException si el nombre ya esta en uso (E01) o es invalido
     * (E06)
     * @throws RemoteException si hay fallo de comunicacion RMI
     */
    void registrarUsuario(String nombreUsuario, ObservadorSubasta observador)
            throws SubastaException, RemoteException;

    /**
     * Crea una nueva subasta con los parametros indicados.
     *
     * @param nombreUsuario usuario que crea la subasta (debe estar registrado)
     * @param objeto descripcion del objeto subastado
     * @param precioBase precio de salida (> 0)
     * @param duracionSegundos duracion de la subasta (> 0)
     * @return ID de la subasta creada (ej: "SUB-1")
     * @throws SubastaException si el usuario no esta registrado (E02) o los
     * parametros son invalidos
     * @throws RemoteException si hay fallo de comunicacion RMI
     */
    String crearSubasta(String nombreUsuario, String objeto,
            double precioBase, int duracionSegundos)
            throws SubastaException, RemoteException;

    /**
     * Registra una puja de un usuario en una subasta.
     *
     * @param nombreUsuario usuario que puja
     * @param idSubasta identificador de la subasta
     * @param cantidad importe de la puja (debe superar el precio actual)
     * @throws SubastaException si la puja es invalida (E04, E05, E08...) o la
     * subasta no existe (E03)
     * @throws RemoteException si hay fallo de comunicacion RMI
     */
    void pujar(String nombreUsuario, String idSubasta, double cantidad)
            throws SubastaException, RemoteException;

    /**
     * Consulta el estado actual de una subasta.
     *
     * @param idSubasta identificador de la subasta
     * @return InfoSubasta con todos los datos del estado actual
     * @throws SubastaException si la subasta no existe (E03)
     * @throws RemoteException si hay fallo de comunicacion RMI
     */
    InfoSubasta verSubasta(String idSubasta)
            throws SubastaException, RemoteException;

    /**
     * Devuelve la lista de todas las subastas (abiertas y cerradas).
     *
     * @return lista (posiblemente vacia) de InfoSubasta
     * @throws RemoteException si hay fallo de comunicacion RMI
     */
    List<InfoSubasta> listarSubastas() throws RemoteException;

    /**
     * Desregistra un usuario del sistema.
     *
     * @param nombreUsuario usuario que se desconecta
     * @throws RemoteException si hay fallo de comunicacion RMI
     */
    void desconectar(String nombreUsuario) throws RemoteException;
}
