package P2.servidor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import P2.modelo.InfoSubasta;
import P2.modelo.SubastaException;
import P2.remoto.ObservadorSubasta;
import P2.remoto.SubastaServicioRemoto;

//Implementacion del servicio remoto de subastas.
//Extiende UnicastRemoteObject para ser exportado por RMI. Delega toda la
//logica en GestorSubastasRMI (mismo rol que en la P1).
// Cada metodo de esta clase ES la operacion: no hay parsing,
// no hay strings, Java serializa y deserializa los parametros automaticamente.
public class SubastaServicioImpl extends UnicastRemoteObject
        implements SubastaServicioRemoto {

    private static final long serialVersionUID = 1L;

    private final GestorSubastasRMI gestor;

    public SubastaServicioImpl() throws RemoteException {
        super();
        this.gestor = new GestorSubastasRMI();
    }

    @Override
    public void registrarUsuario(String nombreUsuario, ObservadorSubasta observador) throws SubastaException, RemoteException {

        gestor.registrarUsuario(nombreUsuario, observador);
        System.out.println("[SERVIDOR] Registrado: " + nombreUsuario);
    }

    @Override
    public String crearSubasta(String nombreUsuario, String objeto, double precioBase, int duracionSegundos) throws SubastaException, RemoteException {

        // Verificar que el usuario esta registrado
        if (!gestor.existeUsuario(nombreUsuario)) {
            throw new SubastaException(SubastaException.NO_REGISTRADO,
                    "Debes registrarte antes de crear una subasta");
        }

        // Comprobaciones de cara caracteristica de la nueva subasta
        if (objeto == null || objeto.trim().isEmpty()) {
            throw new SubastaException(SubastaException.PARAMETROS_INVALIDOS,
                    "El nombre del objeto no puede estar vacio");
        }
        if (precioBase <= 0) {
            throw new SubastaException(SubastaException.PRECIO_INVALIDO,
                    "El precio base debe ser mayor que 0");
        }
        if (duracionSegundos <= 0) {
            throw new SubastaException(SubastaException.PRECIO_INVALIDO,
                    "La duracion debe ser mayor que 0");
        }

        String id = gestor.crearSubasta(objeto.trim(), precioBase, duracionSegundos);
        System.out.println("[SERVIDOR] " + nombreUsuario + " creo subasta " + id);
        return id;
    }

    @Override
    public void pujar(String nombreUsuario, String idSubasta, double cantidad) throws SubastaException, RemoteException {

        if (!gestor.existeUsuario(nombreUsuario)) {
            throw new SubastaException(SubastaException.NO_REGISTRADO,
                    "Debes registrarte antes de pujar");
        }
        if (cantidad <= 0) {
            throw new SubastaException(SubastaException.PRECIO_INVALIDO,
                    "La cantidad debe ser mayor que 0");
        }

        // obtenerSubasta ya lanza SubastaException si no existe
        SubastaRMI subasta = gestor.obtenerSubasta(idSubasta);

        // intentarPujar lanza SubastaException si la puja es invalida
        subasta.intentarPujar(nombreUsuario, cantidad);

        System.out.println("[SERVIDOR] " + nombreUsuario + " puja " + cantidad + " en " + idSubasta);
    }

    @Override
    public InfoSubasta verSubasta(String idSubasta) throws SubastaException, RemoteException {
        SubastaRMI subasta = gestor.obtenerSubasta(idSubasta);
        return subasta.toInfo();
    }

    @Override
    public List<InfoSubasta> listarSubastas() throws RemoteException {
        return gestor.listarSubastas();
    }

    @Override
    public void desconectar(String nombreUsuario) throws RemoteException {
        gestor.desconectarUsuario(nombreUsuario);
        System.out.println("[SERVIDOR] Desconectado: " + nombreUsuario);
    }
}
