package pe.tecsup.porteria.acceso;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.io.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.data.domain.PageRequest;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pe.tecsup.porteria.TestcontainersConfiguration;
import pe.tecsup.porteria.acceso.dto.RegistroFiltro;
import pe.tecsup.porteria.acceso.entity.*;
import pe.tecsup.porteria.acceso.repository.RegistroAccesoRepository;
import pe.tecsup.porteria.acceso.service.RegistroConsultaService;
import pe.tecsup.porteria.dispositivo.entity.*;
import pe.tecsup.porteria.dispositivo.repository.DispositivoRepository;
import pe.tecsup.porteria.auth.repository.UsuarioRepository;
import pe.tecsup.porteria.auth.security.JwtService;
import pe.tecsup.porteria.reporte.service.ReporteService;
@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class) @Transactional
class ReporteTurnoTest {
    @Autowired RegistroAccesoRepository registros; @Autowired DispositivoRepository dispositivos;
    @Autowired RegistroConsultaService consultas; @Autowired ReporteService reportes;
    @Autowired UsuarioRepository usuarios; @Autowired JwtService jwt; @Autowired MockMvc mvc;
    @MockitoBean Clock clock;
    Long dispositivoId;
    @BeforeEach void preparar() {
        when(clock.getZone()).thenReturn(ZoneId.of("America/Lima"));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-05T07:00:00Z"));
        var d=new Dispositivo("Reporte",Punto.PEATONAL);d.setTokenHash("a".repeat(64));
        dispositivoId=dispositivos.saveAndFlush(d).getId();
        registro("2026-10-04T21:59:00");registro("2026-10-04T22:00:00");registro("2026-10-05T01:00:00");
    }
    void registro(String fecha) {
        registros.saveAndFlush(RegistroAcceso.builder().dispositivoId(dispositivoId).fechaHora(LocalDateTime.parse(fecha))
                .metodo(MetodoId.DNI).valorLeido("87654321").direccion(Direccion.ENTRADA).resultado(Resultado.NO_AUTORIZADO).build());
    }
    @Test void turnoDeMadrugadaEmpiezaElDiaAnterior() {
        assertThat(consultas.turno(PageRequest.of(0,20)).getTotalElements()).isEqualTo(2);
    }
    @Test void filtraPorFechasYExportaExcelConFechasNumericas() throws Exception {
        var filtro=new RegistroFiltro(LocalDateTime.parse("2026-10-04T22:00:00"),LocalDateTime.parse("2026-10-05T02:00:00"),
                null,dispositivoId,MetodoId.DNI,Resultado.NO_AUTORIZADO,Direccion.ENTRADA);
        var output=new ByteArrayOutputStream(); reportes.exportar(filtro,output);
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            var sheet=workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(1).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.parse("2026-10-04T22:00:00"));
        }
        String admin="Bearer "+jwt.generar(usuarios.findByUsername("admin.dev").orElseThrow().getId());
        mvc.perform(get("/api/admin/registros").header("Authorization",admin).param("desde","2026-10-04T22:00:00")
                .param("dispositivoId",dispositivoId.toString())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/admin/registros/exportar").header("Authorization",admin).param("desde","2026-01-01T00:00:00")
                .param("hasta","2026-03-01T00:00:00")).andExpect(status().isUnprocessableEntity());
    }
    @Test void exportacionAsyncMantieneLaAutorizacion() throws Exception {
        String admin="Bearer "+jwt.generar(usuarios.findByUsername("admin.dev").orElseThrow().getId());
        MvcResult pending=mvc.perform(get("/api/admin/registros/exportar").header("Authorization",admin)
                .param("desde","2026-10-05T00:00:00").param("hasta","2026-10-05T23:59:00"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(pending)).andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(pending.getResponse().getContentAsByteArray()))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("ID");
        }
    }
}
