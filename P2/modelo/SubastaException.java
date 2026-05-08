package subasta.modelo;

import java.io.Serializable;


public class SubastaException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    // Codigos de error (misma semantica que los Exx del protocolo original)
    public static final String USUARIO_DUPLICADO = "E01";
    public static final String NO_REGISTRADO = "E02";
    public static final String SUBASTA_INEXISTENTE = "E03";
    public static final String SUBASTA_CERRADA = "E04";
    public static final String PUJA_INSUFICIENTE = "E05";
    public static final String PARAMETROS_INVALIDOS = "E06";
    public static final String PRECIO_INVALIDO = "E07";
    public static final String AUTOPUJA = "E08";

    private final String codigo;

    public SubastaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    @Override
    public String toString() {
        return "[" + codigo + "] " + getMessage();
    }
}
