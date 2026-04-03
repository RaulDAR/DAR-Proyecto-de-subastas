package servidor;

import comun.Mensaje;
import java.io.*;
import java.net.Socket;

// Hilo dedicado a gestionar la conexion con un unico cliente TCP.
// Cada vez que un cliente se conecta, el servidor lanza un hilo de este tipo.
public class ManejadorCliente extends Thread {

    // Socket de la conexion con este cliente
    private final Socket socketCliente;

    // Referencia al gestor global (estado compartido del servidor)
    private final GestorSubastas gestor;

    // Nombre del usuario registrado en esta sesion (null hasta que haga REGISTRAR)
    private String nombreUsuario = null;

    // Flujos de entrada y salida con este cliente
    private BufferedReader entrada;
    private PrintWriter salida;

    // Constructor
    public ManejadorCliente(Socket socketCliente, GestorSubastas gestor) {
        this.socketCliente = socketCliente;
        this.gestor = gestor;
        // El nombre del hilo ayuda a depurar cuando hay muchos clientes
        setName("cliente-" + socketCliente.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        System.out.println("[SERVIDOR] Nueva conexion desde: " + socketCliente.getRemoteSocketAddress());

        try {
            // Configurar los flujos de entrada/salida sobre el socket TCP
            entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            // autoFlush=true para que cada println envie inmediatamente
            salida  = new PrintWriter(new OutputStreamWriter(socketCliente.getOutputStream()), true);

            // Bucle principal: leer mensajes del cliente hasta que se desconecte
            String lineaRecibida;
            while ((lineaRecibida = entrada.readLine()) != null) {
                // Procesar el mensaje y obtener la respuesta
                String respuesta = procesarMensaje(lineaRecibida.trim());
                if (respuesta != null) {
                    salida.print(respuesta);
                    salida.flush();
                }

                // Si el cliente pidio desconectarse, salir del bucle
                if (lineaRecibida.trim().startsWith(Mensaje.DESCONECTAR)) {
                    break;
                }
            }

        } catch (IOException e) {
            // Desconexion inesperada del cliente (cable, crash, etc.)
            System.err.println("[SERVIDOR] Desconexion inesperada de " + nombreUsuario
                    + " (" + socketCliente.getRemoteSocketAddress() + "): " + e.getMessage());
        } finally {
            // Limpiar recursos independientemente de como haya terminado
            limpiarConexion();
        }
    }

    // Procesa un mensaje recibido y devuelve la respuesta adecuada segun el protocolo
    private String procesarMensaje(String mensajeRaw) {
        if (mensajeRaw == null || mensajeRaw.isEmpty()) {
            return Mensaje.error(Mensaje.ERR_PARAMETROS, "Mensaje vacio recibido");
        }

        // Dividir el mensaje en campos por el separador '|'
        String[] campos = Mensaje.dividir(mensajeRaw);
        if (campos.length == 0) {
            return Mensaje.error(Mensaje.ERR_PARAMETROS, "Formato de mensaje invalido");
        }

        String tipo = campos[0];

        // Enrutar segun el tipo de mensaje
        switch (tipo) {
            case Mensaje.REGISTRAR:
                return manejarRegistrar(campos);
            case Mensaje.CREAR_SUBASTA:
                return manejarCrearSubasta(campos);
            case Mensaje.PUJAR:
                return manejarPujar(campos);
            case Mensaje.VER_SUBASTA:
                return manejarVerSubasta(campos);
            case Mensaje.LISTAR_SUBASTAS:
                return manejarListarSubastas();
            case Mensaje.DESCONECTAR:
                return manejarDesconectar();
            default:
                return Mensaje.error(Mensaje.ERR_PARAMETROS, "Tipo de mensaje desconocido: " + tipo);
        }
    }

    // Gestiona el registro de un nuevo usuario
    // Formato: REGISTRAR|nombreUsuario
    private String manejarRegistrar(String[] campos) {
        if (campos.length < 2 || campos[1].trim().isEmpty()) {
            return Mensaje.error(Mensaje.ERR_PARAMETROS, "Uso: REGISTRAR|nombreUsuario");
        }

        // Si ya estaba registrado en esta sesion, rechazar
        if (nombreUsuario != null) {
            return Mensaje.error(Mensaje.ERR_PARAMETROS,
                    "Ya estas registrado como '" + nombreUsuario + "'");
        }

        String nombre = campos[1].trim();

        // Intentar registrar en el gestor global
        String errorRegistro = gestor.registrarUsuario(nombre, salida);
        if (errorRegistro != null) {
            return errorRegistro;
        }

        // Registro correcto: guardar el nombre para esta sesion
        nombreUsuario = nombre;
        setName("cliente-" + nombre); // renombrar el hilo para depuracion
        return Mensaje.ok("Bienvenido, " + nombre + ". Estas registrado en el sistema de subastas");
    }

    // Gestiona la creacion de una nueva subasta
    // Formato: CREAR_SUBASTA|objeto|precioBase|duracionSegundos
    private String manejarCrearSubasta(String[] campos) {
        // Verificar que el usuario esta registrado
        if (nombreUsuario == null) {
            return Mensaje.error(Mensaje.ERR_NO_REGISTRADO,
                    "Debes registrarte antes de crear una subasta");
        }

        if (campos.length < 4) {
            return Mensaje.error(Mensaje.ERR_PARAMETROS,
                    "Uso: CREAR_SUBASTA|objeto|precioBase|duracionSegundos");
        }

        String objeto = campos[1].trim();
        double precioBase;
        int duracion;

        // Validar que precio y duracion son numericos y positivos
        try {
            precioBase = Double.parseDouble(campos[2].trim());
            duracion   = Integer.parseInt(campos[3].trim());
        } catch (NumberFormatException e) {
            return Mensaje.error(Mensaje.ERR_PRECIO_INVALIDO,
                    "El precio y la duracion deben ser numeros validos");
        }

        if (precioBase <= 0 || duracion <= 0) {
            return Mensaje.error(Mensaje.ERR_PRECIO_INVALIDO,
                    "El precio base y la duracion deben ser mayores que 0");
        }

        if (objeto.isEmpty()) {
            return Mensaje.error(Mensaje.ERR_PARAMETROS,
                    "El nombre del objeto no puede estar vacio");
        }

        // Crear la subasta en el gestor
        String idSubasta = gestor.crearSubasta(objeto, precioBase, duracion);
        return Mensaje.ok("Subasta creada con exito. ID: " + idSubasta
                + " | Objeto: " + objeto
                + " | Precio base: " + precioBase
                + " | Duracion: " + duracion + "s");
    }

    // Gestiona una puja sobre una subasta existente
    // Formato: PUJAR|idSubasta|cantidad
    private String manejarPujar(String[] campos) {
        // Verificar registro
        if (nombreUsuario == null) {
            return Mensaje.error(Mensaje.ERR_NO_REGISTRADO,
                    "Debes registrarte antes de pujar");
        }

        if (campos.length < 3) {
            return Mensaje.error(Mensaje.ERR_PARAMETROS,
                    "Uso: PUJAR|idSubasta|cantidad");
        }

        String idSubasta = campos[1].trim();
        double cantidad;

        // Validar que la cantidad es un numero
        try {
            cantidad = Double.parseDouble(campos[2].trim());
        } catch (NumberFormatException e) {
            return Mensaje.error(Mensaje.ERR_PRECIO_INVALIDO,
                    "La cantidad debe ser un numero valido");
        }

        if (cantidad <= 0) {
            return Mensaje.error(Mensaje.ERR_PRECIO_INVALIDO,
                    "La cantidad debe ser mayor que 0");
        }

        // Buscar la subasta
        Subasta subasta = gestor.obtenerSubasta(idSubasta);
        if (subasta == null) {
            return Mensaje.error(Mensaje.ERR_SUBASTA_INEXISTENTE,
                    "No existe ninguna subasta con ID: " + idSubasta);
        }

        // Intentar pujar (la subasta valida las reglas internamente)
        String errorPuja = subasta.intentarPujar(nombreUsuario, cantidad);
        if (errorPuja != null) {
            return errorPuja;
        }

        // Puja aceptada
        return Mensaje.ok("Puja de " + cantidad + " aceptada en subasta " + idSubasta
                + ". Eres el mejor postor");
    }

    // Gestiona la consulta del estado de una subasta
    // Formato: VER_SUBASTA|idSubasta
    private String manejarVerSubasta(String[] campos) {
        if (nombreUsuario == null) {
            return Mensaje.error(Mensaje.ERR_NO_REGISTRADO,
                    "Debes registrarte para consultar subastas");
        }

        if (campos.length < 2) {
            return Mensaje.error(Mensaje.ERR_PARAMETROS,
                    "Uso: VER_SUBASTA|idSubasta");
        }

        String idSubasta = campos[1].trim();
        Subasta subasta = gestor.obtenerSubasta(idSubasta);

        if (subasta == null) {
            return Mensaje.error(Mensaje.ERR_SUBASTA_INEXISTENTE,
                    "No existe ninguna subasta con ID: " + idSubasta);
        }

        return Mensaje.ok(subasta.obtenerEstado());
    }

    // Gestiona el listado de todas las subastas
    // Formato: LISTAR_SUBASTAS
    private String manejarListarSubastas() {
        if (nombreUsuario == null) {
            return Mensaje.error(Mensaje.ERR_NO_REGISTRADO,
                    "Debes registrarte para ver las subastas");
        }

        var lista = gestor.listarSubastas();

        if (lista.isEmpty()) {
            return Mensaje.ok("No hay subastas disponibles en este momento");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Lista de subastas (").append(lista.size()).append(") ===\n");
        for (Subasta s : lista) {
            sb.append("[").append(s.getId()).append("] ");
            sb.append(s.getObjeto()).append(" | ");
            sb.append("Precio actual: ").append(s.getPrecioActual()).append(" | ");
            sb.append(s.isAbierta()
                    ? "ABIERTA - " + s.getTiempoRestante() + "s restantes"
                    : "CERRADA - Ganador: " + (s.getMejorPostor() != null ? s.getMejorPostor() : "Nadie"));
            sb.append("\n");
        }

        return Mensaje.ok(sb.toString().trim());
    }

    // Gestiona la desconexion voluntaria del cliente
    // Formato: DESCONECTAR
    private String manejarDesconectar() {
        String respuesta = Mensaje.ok("Hasta luego, " + (nombreUsuario != null ? nombreUsuario : "desconocido"));
        // limpiarConexion() se llamara en el bloque finally de run()
        return respuesta;
    }

    // Cierra el socket y desregistra al usuario del gestor
    private void limpiarConexion() {
        gestor.desconectarUsuario(nombreUsuario);
        try {
            if (!socketCliente.isClosed()) {
                socketCliente.close();
            }
        } catch (IOException e) {
            System.err.println("[SERVIDOR] Error al cerrar socket de " + nombreUsuario + ": " + e.getMessage());
        }
        System.out.println("[SERVIDOR] Conexion cerrada para: " + (nombreUsuario != null ? nombreUsuario : "desconocido"));
    }
}
