package comun;

// Clase que representa un mensaje del protocolo SASP (Sistema de subastas con sockets y protocolo)
// Formato: TIPO|campo1|campo2|...\n
// Los mensajes se separan por salto de linea y los campos por el caracter '|'
public class Mensaje {

    // Tipos de mensajes del cliente al servidor
    public static final String REGISTRAR       = "REGISTRAR";       // REGISTRAR|nombreUsuario
    public static final String CREAR_SUBASTA   = "CREAR_SUBASTA";   // CREAR_SUBASTA|objeto|precioBase|duracionSegundos
    public static final String PUJAR           = "PUJAR";           // PUJAR|idSubasta|cantidad
    public static final String VER_SUBASTA     = "VER_SUBASTA";     // VER_SUBASTA|idSubasta
    public static final String LISTAR_SUBASTAS = "LISTAR_SUBASTAS"; // LISTAR_SUBASTAS
    public static final String DESCONECTAR     = "DESCONECTAR";     // DESCONECTAR

    // Tipos de mensajes del servidor al cliente
    public static final String OK              = "OK";              // OK|mensaje
    public static final String ERROR           = "ERROR";           // ERROR|codigoError|descripcion
    public static final String NOTIFICACION    = "NOTIFICACION";    // NOTIFICACION|tipo|idSubasta|datos...

    // Tipos de notificacion asincrona enviadas por el servidor
    public static final String NOTIF_NUEVA_PUJA    = "NUEVA_PUJA";    // alguien ha pujado
    public static final String NOTIF_TIEMPO_EXT    = "TIEMPO_EXTENDIDO"; // el tiempo se ha extendido
    public static final String NOTIF_SUBASTA_CERRADA = "SUBASTA_CERRADA"; // subasta finalizada

    // Codigos de error definidos en el protocolo
    public static final String ERR_USUARIO_DUPLICADO  = "E01"; // nombre de usuario ya en uso
    public static final String ERR_NO_REGISTRADO      = "E02"; // cliente no registrado
    public static final String ERR_SUBASTA_INEXISTENTE = "E03"; // id de subasta no existe
    public static final String ERR_SUBASTA_CERRADA    = "E04"; // la subasta ya ha cerrado
    public static final String ERR_PUJA_INSUFICIENTE  = "E05"; // la puja no supera el minimo
    public static final String ERR_PARAMETROS         = "E06"; // faltan campos o formato incorrecto
    public static final String ERR_PRECIO_INVALIDO    = "E07"; // precio o duracion no numericos o negativos
    public static final String ERR_AUTOPUJA           = "E08"; // el mejor postor no puede volver a pujar

    // Separador de campos en el mensaje
    public static final String SEP = "|";

    // Separador de fin de mensaje (delimitador de mensaje sobre el flujo TCP)
    public static final String FIN = "\n";

    // Construye un mensaje uniendo los campos con el separador
    public static String construir(String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            sb.append(campos[i]);
            if (i < campos.length - 1) {
                sb.append(SEP);
            }
        }
        sb.append(FIN);
        return sb.toString();
    }

    // Divide un mensaje recibido en sus campos
    // Se elimina el \n final si existe
    public static String[] dividir(String mensaje) {
        if (mensaje == null) return new String[0];
        mensaje = mensaje.trim();
        return mensaje.split("\\|");
    }

    // Construye un mensaje de respuesta OK con texto informativo
    public static String ok(String texto) {
        return construir(OK, texto);
    }

    // Construye un mensaje de error con codigo y descripcion
    public static String error(String codigo, String descripcion) {
        return construir(ERROR, codigo, descripcion);
    }

    // Construye una notificacion asincrona del servidor
    public static String notificacion(String tipo, String... datos) {
        String[] campos = new String[2 + datos.length];
        campos[0] = NOTIFICACION;
        campos[1] = tipo;
        for (int i = 0; i < datos.length; i++) {
            campos[2 + i] = datos[i];
        }
        return construir(campos);
    }
}
