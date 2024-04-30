package lsi.ubu.excepciones;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CompraBilleteTrenException: Implementa las excepciones contextualizadas de la
 * transaccion de CompraBilleteTren
 * 
 * @author <a href="mailto:jmaudes@ubu.es">Jesús Maudes</a>
 * @author <a href="mailto:rmartico@ubu.es">Raúl Marticorena</a>
 * @version 1.0
 * @since 1.0
 */
public class CompraBilleteTrenException extends SQLException {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CompraBilleteTrenException.class);

    public static final int NO_PLAZAS = 1;
    public static final int NO_EXISTE_VIAJE = 2;

    private int codigo; // = -1
    private String mensaje;

    public CompraBilleteTrenException(int code) {
        this.codigo = code;
        switch (code) {
            case NO_PLAZAS:
                this.mensaje = "No hay plazas suficientes.";
                break;
            case NO_EXISTE_VIAJE:
                this.mensaje = "No existe viaje para tal fecha, hora, origen y destino.";
                break;
            default:
                this.mensaje = "Error desconocido en la transacción de compra de billete de tren.";
                break;
        }

        LOGGER.debug(mensaje);

        // Traza de pila
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            LOGGER.debug(ste.toString());
        }
    }

    @Override
    public String getMessage() { //Redefinición del metodo de la clase Exception
        return mensaje;
    }

    @Override
    public int getErrorCode() { //Redefinición del metodo de la clase Exception
        return codigo;
    }
}