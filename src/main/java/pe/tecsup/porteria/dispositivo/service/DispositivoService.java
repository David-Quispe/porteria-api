package pe.tecsup.porteria.dispositivo.service;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import pe.tecsup.porteria.dispositivo.entity.Dispositivo;
import pe.tecsup.porteria.dispositivo.dto.*;
import pe.tecsup.porteria.dispositivo.repository.DispositivoRepository;
import pe.tecsup.porteria.dispositivo.security.DispositivoAutenticado;
import pe.tecsup.porteria.shared.dto.*;
import pe.tecsup.porteria.shared.exception.NotFoundException;
@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class DispositivoService {
    private final DispositivoRepository repository;
    private final DispositivoTokenService tokens;
    private final Clock clock;
    public PageResponse<DispositivoResponse> listar(int page,int size) {
        return PageResponse.of(repository.findAll(Paginacion.of(page,size,Sort.by("id"))).map(DispositivoResponse::of));
    }
    public DispositivoResponse obtener(Long id) {
        return DispositivoResponse.of(repository.findById(id).orElseThrow(() -> new NotFoundException("Dispositivo",id)));
    }
    @Transactional
    public TokenResponse crear(DispositivoRequest r) {
        Dispositivo d=new Dispositivo(r.nombre().strip(),r.punto());
        String token=tokens.asignarNuevoToken(d);
        repository.saveAndFlush(d);
        return new TokenResponse(DispositivoResponse.of(d),token);
    }
    @Transactional
    public DispositivoResponse editar(Long id,DispositivoRequest r) {
        Dispositivo d=bloquear(id); d.setNombre(r.nombre().strip()); d.setPunto(r.punto());
        return DispositivoResponse.of(d);
    }
    @Transactional
    public void estado(Long id,boolean activo) { bloquear(id).setActivo(activo); }
    @Transactional
    public TokenResponse regenerar(Long id) {
        Dispositivo d=bloquear(id); String token=tokens.asignarNuevoToken(d);
        repository.flush();
        return new TokenResponse(DispositivoResponse.of(d),token);
    }
    /** Mantiene el bloqueo hasta terminar la transacción exterior de la lectura. */
    @Transactional
    public Dispositivo bloquearActivo(DispositivoAutenticado principal) {
        Dispositivo d=bloquear(principal.id());
        if (!d.isActivo() || !d.getTokenHash().equals(principal.tokenHash())) throw new BadCredentialsException("Dispositivo inválido");
        return d;
    }
    @Transactional
    public LocalDateTime ping(DispositivoAutenticado principal) {
        Dispositivo d=bloquearActivo(principal); LocalDateTime hora=LocalDateTime.now(clock); d.setUltimoPing(hora); return hora;
    }
    private Dispositivo bloquear(Long id) {
        return repository.bloquear(id).orElseThrow(() -> new NotFoundException("Dispositivo",id));
    }
}
