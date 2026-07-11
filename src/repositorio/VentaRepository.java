package repositorio;

import java.util.List;
import modelo.Cliente;
import modelo.Concierto;
import modelo.Venta;

public interface VentaRepository {

    // Persiste, en una única transacción ACID, la venta ya confirmada en memoria (Cliente.comprar
    // ya reservó las entradas y validó las reglas de negocio): INSERT de la venta, INSERT por lotes
    // de sus entradas, UPDATE optimista de zonas.version, y UPDATE del saldo de puntos del cliente.
    boolean guardarCompraCompleta(Cliente cliente, Concierto concierto, Venta venta);

    // Historial completo de compras de un cliente, hidratado con un solo JOIN (ventas + entradas)
    // para evitar el problema N+1 de una consulta de entradas por cada venta.
    List<Venta> cargarHistorialPorCliente(String dniCliente, List<Concierto> conciertosEnMemoria);

    // Reversa persistida de una venta ya anulada en memoria (Cliente.anularVenta ya liberó las
    // entradas y ajustó los puntos): marca venta y entradas como CANCELLED y guarda el nuevo saldo.
    boolean anularVentaPersistida(Cliente cliente, Venta venta);

    // Vista de solo lectura para el panel de administrador: todas las ventas pagadas de todos los
    // clientes, en un solo JOIN (ventas+usuarios+zonas+conciertos). Cada fila: {cliente, zona,
    // monto, concierto}. No usa Cliente.getVentas() porque ese historial solo se hidrata para el
    // cliente que inició sesión, no para todos a la vez.
    List<Object[]> cargarResumenVentasParaAdmin();
}
