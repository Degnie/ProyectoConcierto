package repositorio;

import java.util.ArrayList;
import java.util.List;
import modelo.Cliente;

public class MemoriaClienteRepository implements ClienteRepository {
    private List<Cliente> clientes;

    public MemoriaClienteRepository() {
        this.clientes = new ArrayList<>();
    }

    @Override
    public boolean save(Cliente cliente) {
        if (cliente == null) return false;
        // Si ya existe, lo actualizamos (simula INSERT / UPDATE)
        Cliente existente = findByDni(cliente.getDni());
        if (existente != null) {
            clientes.remove(existente);
        }
        clientes.add(cliente);
        return true;
    }

    @Override
    public Cliente findByDni(String dni) {
        for (Cliente c : clientes) {
            if (c.getDni().equals(dni)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public List<Cliente> findAll() {
        return new ArrayList<>(clientes);
    }
}
