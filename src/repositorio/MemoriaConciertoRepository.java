package repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import modelo.Concierto;

public class MemoriaConciertoRepository implements ConciertoRepository {
    private List<Concierto> conciertos;

    public MemoriaConciertoRepository() {
        this.conciertos = new ArrayList<>();
    }

    public MemoriaConciertoRepository(List<Concierto> iniciales) {
        this.conciertos = iniciales;
    }

    @Override
    public boolean save(Concierto concierto) {
        if (concierto == null) return false;
        Concierto existente = findById(concierto.getId());
        if (existente != null) {
            conciertos.remove(existente);
        }
        conciertos.add(concierto);
        return true;
    }

    @Override
    public Concierto findById(UUID id) {
        for (Concierto c : conciertos) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Concierto findByNombre(String nombre) {
        for (Concierto c : conciertos) {
            if (c.getNombre().equalsIgnoreCase(nombre)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public List<Concierto> findAll() {
        return new ArrayList<>(conciertos);
    }

    @Override
    public boolean delete(UUID id) {
        Concierto c = findById(id);
        if (c != null) {
            return conciertos.remove(c);
        }
        return false;
    }
}
