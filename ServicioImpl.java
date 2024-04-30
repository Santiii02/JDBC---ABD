package lsi.ubu.servicios;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.CompraBilleteTrenException;
import lsi.ubu.util.PoolDeConexiones;

public class ServicioImpl implements Servicio {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);
    
    @Override
    public void anularBillete(Time hora, Date fecha, String origen, String destino, int nroPlazas, int ticket) 
            throws SQLException {
        PoolDeConexiones pool = PoolDeConexiones.getInstance();
        
        java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
        java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());
        
        Connection con = null;
        PreparedStatement st = null;
        
        try {
            con = pool.getConnection();
            con.setAutoCommit(false);

            // Verificar si el viaje existe para la fecha, hora, origen y destino especificados
            st = con.prepareStatement("SELECT * FROM viajes v INNER JOIN recorridos r ON v.idRecorrido = r.idRecorrido " +
                                      "WHERE v.fecha = ? AND r.horaSalida = ? AND r.estacionOrigen = ? AND r.estacionDestino = ?");
            st.setDate(1, fechaSqlDate);
            st.setTimestamp(2, horaTimestamp);
            st.setString(3, origen);
            st.setString(4, destino);
            ResultSet rs = st.executeQuery();

            if (!rs.next()) {
                throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
            }

            // Realizar la anulación de billetes
            // Actualizar el número de plazas disponibles para el viaje indicado
            st = con.prepareStatement("UPDATE viajes SET nPlazasLibres = nPlazasLibres + ? WHERE idViaje = ?");
            st.setInt(1, nroPlazas);
            st.setInt(2, ticket);
            // Ejecutar la consulta
            st.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
        } finally {
            // Cerrar recursos
            if (st != null) {
                st.close();
            }
            if (con != null) {
                con.close();
            }
        }
    }
    
    @Override
    public void comprarBillete(Time hora, Date fecha, String origen, String destino, int nroPlazas) throws SQLException {
        PoolDeConexiones pool = PoolDeConexiones.getInstance();
        Connection con = null;
        PreparedStatement st = null;

        java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());
		
        try {
            con = pool.getConnection();
            con.setAutoCommit(false);

            /* Conversiones de fechas y horas */
            java.sql.Date v_fecha = new java.sql.Date(fecha.getTime());
            java.sql.Timestamp v_hora = new java.sql.Timestamp(hora.getTime());

            // Consulta para obtener el idViaje y el precio
            st = con.prepareStatement("SELECT idViaje, precio FROM recorridos natural join viajes "
                    + "WHERE horaSalida-trunc(horaSalida) = ?-trunc(?) AND trunc(fecha) = trunc(?) "
                    + "AND estacionOrigen = ? AND estacionDestino = ? ");
            st.setTimestamp(1, v_hora);
            st.setTimestamp(2, v_hora);
            st.setDate(3, v_fecha);
            st.setString(4, origen);
            st.setString(5, destino);
            ResultSet rs = st.executeQuery();

            if (!rs.next()) {
                throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
            }

            int idViaje = rs.getInt("idViaje");
            BigDecimal precio = rs.getBigDecimal("precio");

            // Actualizar el número de plazas disponibles en la tabla Viajes
            st = con.prepareStatement("UPDATE viajes SET nPlazasLibres = nPlazasLibres - ? WHERE idViaje = ? AND ? <= nPlazasLibres");
            st.setInt(1, nroPlazas);
            st.setInt(2, idViaje);
            st.setInt(3, nroPlazas);
            int rowCount = st.executeUpdate();

            if (rowCount == 0) {
                throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_PLAZAS);
            }

            // Insertar nuevo ticket en la tabla Tickets
            st = con.prepareStatement("INSERT INTO tickets VALUES (SEQ_TICKETS.nextval, ?, current_date, ?, ?)");
            st.setInt(1, idViaje);
            st.setInt(2, nroPlazas);
            st.setBigDecimal(3, precio.multiply(new BigDecimal(nroPlazas)));
            st.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            // Cerrar recursos
            if (st != null) {
                st.close();
            }
            if (con != null) {
                con.close();
            }
        }
    }
}
