package pe.tecsup.porteria.acceso.service;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.tecsup.porteria.acceso.dto.*;
import pe.tecsup.porteria.acceso.entity.*;
import pe.tecsup.porteria.acceso.event.AccesoRegistradoEvent;
import pe.tecsup.porteria.acceso.repository.RegistroAccesoRepository;
import pe.tecsup.porteria.persona.entity.*;
import pe.tecsup.porteria.persona.service.*;
import pe.tecsup.porteria.dispositivo.service.DispositivoService;
import pe.tecsup.porteria.dispositivo.security.DispositivoAutenticado;
import pe.tecsup.porteria.shared.exception.BusinessException;
@Service @RequiredArgsConstructor @Slf4j
public class AccesoService {
    private final RegistroAccesoRepository registros;
    private final PersonaService personas;
    private final CredencialService credenciales;
    private final DispositivoService dispositivos;
    private final ReglaService reglas;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public LecturaResponse registrar(DispositivoAutenticado principal,LecturaRequest request) {
        var dispositivo=dispositivos.bloquearActivo(principal);
        LocalDateTime ahora=LocalDateTime.now(clock);
        String valor=request.metodo()==MetodoId.NFC ? ValorCredencial.normalizar(TipoCredencial.NFC,request.valor()) : request.valor();
        if (request.metodo()==MetodoId.DNI && !valor.matches("[0-9]{8,15}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,"DNI inválido");
        }
        var anterior=registros.findFirstByDispositivoIdAndMetodoAndValorLeidoAndDireccionAndFechaHoraGreaterThanOrderByFechaHoraDesc(
                dispositivo.getId(),request.metodo(),valor,request.direccion(),ahora.minusSeconds(5));
        if (anterior.isPresent()) {
            var r=anterior.get();
            boolean salidaPosterior=r.getResultado()==Resultado.ENTRADA_REPETIDA && r.getPersonaId()!=null
                    && registros.findFirstByPersonaIdAndResultadoOrderByFechaHoraDescIdDesc(r.getPersonaId(),Resultado.AUTORIZADO)
                            .map(ultimo -> ultimo.getDireccion()==Direccion.SALIDA).orElse(false);
            if ((r.getResultado()!=Resultado.AUTORIZADO || request.direccion()!=Direccion.ENTRADA) && !salidaPosterior) {
                Persona p=r.getPersonaId()==null ? null : personas.buscarPorIds(java.util.Set.of(r.getPersonaId())).stream().findFirst().orElse(null);
                return new LecturaResponse(r.getResultado(),r.getResultado().abre(),nombre(p));
            }
        }
        Persona persona=null; Long credencialId=null;
        if (request.metodo()==MetodoId.DNI) persona=personas.buscarPorDni(valor).orElse(null);
        else {
            var c=credenciales.buscarActiva(TipoCredencial.valueOf(request.metodo().name()),valor).orElse(null);
            if (c!=null) { persona=c.getPersona(); credencialId=c.getId(); }
        }
        Resultado resultado;
        if (persona==null) resultado=Resultado.NO_AUTORIZADO;
        else if (!persona.isActivo()) resultado=Resultado.INACTIVO;
        else if (ahora.toLocalDate().isBefore(persona.getVigenciaInicio())
                || persona.getVigenciaFin()!=null && ahora.toLocalDate().isAfter(persona.getVigenciaFin())) resultado=Resultado.VENCIDO;
        else if (request.direccion()==Direccion.ENTRADA && !reglas.permite(persona.getTipo(),dispositivo.getPunto(),ahora)) resultado=Resultado.FUERA_DE_HORARIO;
        else if (request.direccion()==Direccion.ENTRADA && registros
                .findFirstByPersonaIdAndResultadoOrderByFechaHoraDescIdDesc(persona.getId(),Resultado.AUTORIZADO)
                .map(r -> r.getDireccion()==Direccion.ENTRADA).orElse(false)) resultado=Resultado.ENTRADA_REPETIDA;
        else resultado=Resultado.AUTORIZADO;
        var registro=registros.saveAndFlush(RegistroAcceso.builder().fechaHora(ahora).dispositivoId(dispositivo.getId())
                .personaId(persona==null?null:persona.getId()).credencialId(credencialId).metodo(request.metodo())
                .valorLeido(valor).direccion(request.direccion()).resultado(resultado).build());
        events.publishEvent(new AccesoRegistradoEvent(registro.getId(),ahora,dispositivo.getId(),registro.getPersonaId(),
                nombre(persona),persona==null?null:persona.getFotoUrl(),request.metodo(),request.direccion(),resultado));
        log.info("Acceso dispositivo={} metodo={} direccion={} resultado={}",dispositivo.getId(),request.metodo(),request.direccion(),resultado);
        return new LecturaResponse(resultado,resultado.abre(),nombre(persona));
    }
    private String nombre(Persona p) { return p==null?null:p.getNombres()+" "+p.getApellidos(); }
}
