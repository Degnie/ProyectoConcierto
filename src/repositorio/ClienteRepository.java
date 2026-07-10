package repositorio;

import java.util.List;
import modelo.Cliente;

public interface ClienteRepository {
    boolean save(Cliente cliente);
    Cliente findByDni(String dni);
    List<Cliente> findAll();
}
