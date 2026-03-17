package common;

/**
 * Protocolo para la subasta en línea.
 * En este archivo se definen los mensajes que el cliente y el servidor intercambian, 
 * así como los códigos de error y la política de temporización de las subastas.
 * 
 * 
 *
 * Formato del mensaje:
 * 
 *   COMANDO|campo1|campo2|...\n
 *     - COMANDO: una palabra en mayúsculas que indica la acción o respuesta.
 *     - campoN: datos adicionales separados por el delimitador "|".
 *     - Cada mensaje termina con un salto de línea "\n".
 * 
 * 
 * Formato ABNF:
 * 
 * mensaje = comando 1*("|"campo) CLRF
 * comando = *ALPHA
 * campo = *(%x20-7C / %x7E)
 * 
 */


public class protocolo {

    // Delimitadores

    public static final String DELIMITADOR = "|";
    public static final String TERMINADOR = "\n";
    public static final String ENCODING = "UTF-8";

////////////////////////////////////////////////////////////////////////////////////
    //Comandos del cliente
////////////////////////////////////////////////////////////////////////////////////

    //CREAR_SUBASTA|descripcion|precioInicial|duracionSegundos|incrementoMinimo
    public static final String CREAR_SUBASTA  = "CREAR_SUBASTA";

    //LISTA_SUBASTAS
    public static final String LISTA_SUBASTAS = "LISTA_SUBASTAS";

    //GET_SUBASTA|idSubasta
    public static final String GET_SUBASTA = "GET_SUBASTA";

    //PUJAR|idSubasta|oferta
    public static final String PUJAR = "PUJAR";

    //SEGUIR|idSubasta
    public static final String SEGUIR = "SEGUIR";

    //DEJAR_DE_SEGUIR|idSubasta
    public static final String DEJAR_DE_SEGUIR = "DEJAR_DE_SEGUIR";

    //SALIR 
    public static final String SALIR ="SALIR";

////////////////////////////////////////////////////////////////////////////////////
    //RESPUESTAS del servidor
////////////////////////////////////////////////////////////////////////////////////


    //OK|payload
    public static final String OK = "OK";

    //ERROR|codigo|descripcion
    public static final String ERROR = "ERROR";

////////////////////////////////////////////////////////////////////////////////////
    //Mensajes asíncronos del servidor
////////////////////////////////////////////////////////////////////////////////////

    //NOTIFICACION_SUBASTA|idSubasta|postor|oferta|tiempoRestante 
    public static final String NOTIFICACION_SUBASTA = "NOTIFICACION_SUBASTA";

    //NOTIFICACION_TIEMPO|idSubasta|tiempoRestante (avisa que el )
    public static final String NOTIFICACION_TIEMPO = "NOTIFICACION_TIEMPO"; 

    //NOTIFICACION_CIERRE|idSubasta|ganador|precioFinal|ofertasTotales (avisa que la subasta ha sido finalizada)
    public static final String NOTIFICACION_CIERRE = "NOTIFICACION_CIERRE";

    //NOTIFICACION_EXTENDIDO|idSubasta|newTimeRemaining|reason (avisa de que el tiempo de la subasta ha sido extendido)
    public static final String NOTIFICACION_EXTENDIDO = "NOTIFICACION_EXTENDIDO";

///////////////////////////////////////////////////////////////////////////////////
///Códigos de error
/// ////////////////////////////////////////////////////////////////////////////////


    public static final String ERR_DESCONOCIDO_CMD = "E001";
    public static final String ERR_MALFORMADO = "E002";
    public static final String ERR_SUBASTA_NO_ENCONTRADA = "E003";
    public static final String ERR_SUBASTA_CERRADA = "E004";
    public static final String ERR_PUJA_DEMASIADO_BAJA = "E005";
    public static final String ERR_PUJA_TARDE = "E006";
    public static final String ERR_PARAMETRO_INVALIDO = "E007";
    public static final String ERR_NO_SUSCRITO = "E008";


    ///////////////////////////////////////////////////////////////////////////
    //  Política temporal de las subastas
    ///////////////////////////////////////////////////////////////////////////
    /** Si llega una puja quedando menos de "UMBRAL_DE_TIEMPO" se amplía el timepo de la puja
    durante "EXTENSION_EN_SEGUNDOS" */
    public static final int UMBRAL_DE_TIEMPO = 30;
    public static final int EXTENSION_EN_SEGUNDOS = 60;

    // ------------------------------------------------------------------ //
    //  Helper builders
    // ------------------------------------------------------------------ //
    public static String ok(String payload) {
        return OK + DELIMITADOR + payload + TERMINADOR;
    }

    public static String error(String code, String descripcion) {
        return ERROR + DELIMITADOR + code + DELIMITADOR + descripcion + TERMINADOR;
    }

    public static String join(String... parts) {
        return String.join(DELIMITADOR, parts) + TERMINADOR;
    }
}


