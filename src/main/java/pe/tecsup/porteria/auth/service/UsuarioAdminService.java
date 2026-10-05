package pe.tecsup.porteria.auth.service;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.persistence.EntityManager;
import pe.tecsup.porteria.auth.dto.*;
import pe.tecsup.porteria.auth.entity.*;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;
import pe.tecsup.porteria.shared.dto.*;
import pe.tecsup.porteria.shared.exception.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class UsuarioAdminService {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final EntityManager entityManager;
    public PageResponse<UsuarioResponse> listar(int page,int size) {
        return PageResponse.of(repository.findAll(Paginacion.of(page,size,Sort.by("id"))).map(UsuarioResponse::of));
    }
    public UsuarioResponse obtener(Long id) { return UsuarioResponse.of(entidad(id)); }
    @Transactional public UsuarioResponse crear(UsuarioRequest r) {
        bloquearAdministracion();
        return UsuarioResponse.of(repository.saveAndFlush(new Usuario(r.username(),hash(r.password()),r.nombre().strip(),r.rol())));
    }
    @Transactional public UsuarioResponse editar(Long id,UsuarioUpdate r) {
        bloquearAdministracion();var u=entidad(id); protegerUltimoAdmin(u,r.rol(),u.isActivo());
        u.setNombre(r.nombre().strip());u.setRol(r.rol());return UsuarioResponse.of(u);
    }
    @Transactional public void estado(Long id,boolean activo) {
        bloquearAdministracion();var u=entidad(id);protegerUltimoAdmin(u,u.getRol(),activo);u.setActivo(activo);
    }
    @Transactional public void password(Long id,String password) {
        bloquearAdministracion();var u=entidad(id);u.setPasswordHash(hash(password));u.setSesionVersion(u.getSesionVersion()+1);
    }
    private String hash(String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72) throw new BusinessException(HttpStatus.BAD_REQUEST,"La contraseña excede 72 bytes UTF-8");
        return encoder.encode(password);
    }
    private void bloquearAdministracion() {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(82641001)").getResultList();
    }
    private void protegerUltimoAdmin(Usuario u,Rol rol,boolean activo) {
        if(u.isActivo() && u.getRol()==Rol.ADMIN && (!activo || rol!=Rol.ADMIN) && repository.countByRolAndActivoTrue(Rol.ADMIN)<=1)
            throw new BusinessException("Debe conservarse al menos un administrador activo");
    }
    private Usuario entidad(Long id) {return repository.findById(id).orElseThrow(()->new NotFoundException("Usuario",id));}
}
