package repositorio;

import java.util.List;
import modelo.Cliente;
import modelo.Tarjeta;

public interface ClienteRepository {
    boolean save(Cliente cliente);
    Cliente findByDni(String dni);
    List<Cliente> findAll();

    // "Guardar tarjeta para futuras compras": persiste tipo/enmascarado/fecha/token para que
    // findByDni la hidrate de nuevo en el próximo login. Nunca recibe el número completo ni el CVV.
    boolean guardarTarjeta(String dni, Tarjeta tarjeta, String paymentToken);
}
