package repositorio;

import modelo.CredencialAdmin;

public interface UsuarioRepository {
    // null si el dni no existe o no tiene rol ADMIN
    CredencialAdmin buscarAdminPorDni(String dni);
}
