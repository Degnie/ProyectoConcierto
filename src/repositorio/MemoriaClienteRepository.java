package repositorio;

import java.util.ArrayList;
import java.util.List;
import modelo.Cliente;
import modelo.Tarjeta;

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

    // En memoria, findByDni ya devuelve la misma instancia que registrarTarjeta() mutó; esta
    // "persistencia" solo existe para cumplir el contrato de ClienteRepository en pruebas.
    @Override
    public boolean guardarTarjeta(String dni, Tarjeta tarjeta, String paymentToken) {
        Cliente existente = findByDni(dni);
        if (existente == null) return false;
        existente.hidratarTarjeta(tarjeta, paymentToken);
        return true;
    }
}
