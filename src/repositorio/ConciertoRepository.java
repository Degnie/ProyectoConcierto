package repositorio;

import java.util.List;
import java.util.UUID;
import modelo.Concierto;

public interface ConciertoRepository {
    boolean save(Concierto concierto);
    Concierto findById(UUID id);
    Concierto findByNombre(String nombre);
    List<Concierto> findAll();
    boolean delete(UUID id);
}
