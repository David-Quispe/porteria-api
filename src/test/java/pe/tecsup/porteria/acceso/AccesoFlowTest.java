package pe.tecsup.porteria.acceso;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.acceso.dto.*;
import pe.tecsup.porteria.acceso.entity.*;
import pe.tecsup.porteria.acceso.service.*;
import pe.tecsup.porteria.acceso.repository.RegistroAccesoRepository;
import pe.tecsup.porteria.dispositivo.dto.DispositivoRequest;
import pe.tecsup.porteria.dispositivo.entity.Punto;
import pe.tecsup.porteria.dispositivo.repository.DispositivoRepository;
import pe.tecsup.porteria.dispositivo.service.DispositivoService;
import pe.tecsup.porteria.dispositivo.security.DispositivoAutenticado;
import pe.tecsup.porteria.persona.entity.*;
import pe.tecsup.porteria.persona.repository.PersonaRepository;
@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)
class AccesoFlowTest {
    @Autowired JdbcTemplate jdbc; @Autowired AccesoService accesos; @Autowired ReglaService reglas;
    @Autowired PersonaRepository personas; @Autowired DispositivoService dispositivos;
    @Autowired DispositivoRepository dispositivoRepository; @Autowired RegistroAccesoRepository registros;
    @Autowired Clock clock; @Autowired MockMvc mvc;
    DispositivoAutenticado principal; String token;
    @BeforeEach void preparar() {
        for(String tabla:List.of("registro_acceso","credencial","regla_acceso","persona","dispositivo")) jdbc.update("delete from "+tabla);
        var creado=dispositivos.crear(new DispositivoRequest("Puerta",Punto.PEATONAL)); token=creado.token();
        var d=dispositivoRepository.findById(creado.dispositivo().id()).orElseThrow();
        principal=new DispositivoAutenticado(d.getId(),d.getTokenHash());
    }
    Persona persona() {
        var p=new Persona(TipoPersona.VISITANTE,"87654321","Ana","Quispe");
        p.setVigenciaInicio(LocalDate.now(clock).minusDays(1));
        return personas.saveAndFlush(p);
    }
    LecturaRequest entrada() { return new LecturaRequest(MetodoId.DNI,"87654321",Direccion.ENTRADA); }
    @ParameterizedTest @EnumSource(value=Resultado.class,names="ENTRADA_REPETIDA",mode=EnumSource.Mode.EXCLUDE)
    void registraLosCincoResultados(Resultado esperado) {
        if(esperado!=Resultado.NO_AUTORIZADO) {
            var p=persona();
            if(esperado==Resultado.INACTIVO) p.setActivo(false);
            if(esperado==Resultado.VENCIDO) p.setVigenciaFin(LocalDate.now(clock).minusDays(1));
            personas.saveAndFlush(p);
            if(esperado==Resultado.FUERA_DE_HORARIO) reglas.guardar(null,new ReglaRequest(TipoPersona.VISITANTE,null,
                    Set.of(LocalDate.now(clock).getDayOfWeek().plus(1).getValue()),LocalTime.MIN,LocalTime.MAX,"Otro día"));
        }
        var respuesta=accesos.registrar(principal,entrada());
        assertThat(respuesta.resultado()).isEqualTo(esperado);
        assertThat(respuesta.abrir()).isEqualTo(esperado==Resultado.AUTORIZADO);
        assertThat(registros.count()).isEqualTo(1);
    }
    @Test void salidaNoAplicaHorariosYNoSeConfundeConEntrada() {
        persona();
        reglas.guardar(null,new ReglaRequest(TipoPersona.VISITANTE,null,
                Set.of(LocalDate.now(clock).getDayOfWeek().plus(1).getValue()),LocalTime.MIN,LocalTime.MAX,"Otro día"));
        assertThat(accesos.registrar(principal,entrada()).resultado()).isEqualTo(Resultado.FUERA_DE_HORARIO);
        assertThat(accesos.registrar(principal,new LecturaRequest(MetodoId.DNI,"87654321",Direccion.SALIDA)).resultado()).isEqualTo(Resultado.AUTORIZADO);
        assertThat(registros.count()).isEqualTo(2);
    }
    @Test void entradaRepetidaSeRechazaHastaRegistrarSalida() {
        persona();
        assertThat(accesos.registrar(principal,entrada()).resultado()).isEqualTo(Resultado.AUTORIZADO);
        var repetida=accesos.registrar(principal,entrada());
        assertThat(repetida.resultado()).isEqualTo(Resultado.ENTRADA_REPETIDA);
        assertThat(repetida.abrir()).isFalse();
        assertThat(accesos.registrar(principal,entrada()).resultado()).isEqualTo(Resultado.ENTRADA_REPETIDA);
        assertThat(registros.count()).isEqualTo(2);
        assertThat(accesos.registrar(principal,new LecturaRequest(MetodoId.DNI,"87654321",Direccion.SALIDA)).resultado())
                .isEqualTo(Resultado.AUTORIZADO);
        assertThat(accesos.registrar(principal,entrada()).resultado()).isEqualTo(Resultado.AUTORIZADO);
        assertThat(registros.count()).isEqualTo(4);
    }
    @Test void entradaRepetidaSeDetectaEnOtroDispositivo() {
        persona();
        var segundo=dispositivos.crear(new DispositivoRequest("Otra puerta",Punto.PEATONAL));
        var dispositivo=dispositivoRepository.findById(segundo.dispositivo().id()).orElseThrow();
        var otraPuerta=new DispositivoAutenticado(dispositivo.getId(),dispositivo.getTokenHash());
        assertThat(accesos.registrar(principal,entrada()).resultado()).isEqualTo(Resultado.AUTORIZADO);
        assertThat(accesos.registrar(otraPuerta,entrada()).resultado()).isEqualTo(Resultado.ENTRADA_REPETIDA);
    }
    @Test void lecturasSimultaneasSoloCreanUnaAlertaDeEntradaRepetida() throws Exception {
        persona(); CountDownLatch start=new CountDownLatch(1);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<LecturaResponse>> futures=new ArrayList<>();
            for(int i=0;i<6;i++) futures.add(executor.submit(() -> {start.await();return accesos.registrar(principal,entrada());}));
            start.countDown();
            List<Resultado> resultados=new ArrayList<>();
            for(var future:futures) resultados.add(future.get(20,TimeUnit.SECONDS).resultado());
            assertThat(resultados).containsExactlyInAnyOrder(Resultado.AUTORIZADO,Resultado.ENTRADA_REPETIDA,
                    Resultado.ENTRADA_REPETIDA,Resultado.ENTRADA_REPETIDA,Resultado.ENTRADA_REPETIDA,Resultado.ENTRADA_REPETIDA);
        }
        assertThat(registros.count()).isEqualTo(2);
    }
    @Test void contratoHttpValidaTokenYJson() throws Exception {
        persona();
        mvc.perform(post("/api/dispositivo/lecturas").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/dispositivo/lecturas").header("X-Device-Token",token).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/dispositivo/lecturas").header("X-Device-Token",token).contentType("application/json")
                .content("{\"metodo\":\"DNI\",\"valor\":\"87654321\",\"direccion\":\"ENTRADA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.abrir").value(true)).andExpect(jsonPath("$.nombre").value("Ana Quispe"));
    }
}
