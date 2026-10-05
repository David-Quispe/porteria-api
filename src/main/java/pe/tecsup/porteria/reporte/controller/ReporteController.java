package pe.tecsup.porteria.reporte.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.acceso.dto.*;
import pe.tecsup.porteria.acceso.service.RegistroConsultaService;
import pe.tecsup.porteria.reporte.service.ReporteService;
import pe.tecsup.porteria.shared.dto.*;
@RestController @RequestMapping("/api/admin/registros") @RequiredArgsConstructor
@Tag(name="Reportes") @SecurityRequirement(name="jwt")
public class ReporteController {
    private final RegistroConsultaService registros;
    private final ReporteService reportes;
    @Operation(summary = "Consultar historial filtrado")
    @GetMapping public PageResponse<RegistroResponse> listar(@ModelAttribute RegistroFiltro filtro,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return PageResponse.of(registros.listar(filtro,Paginacion.of(page,size,Sort.by(Sort.Direction.DESC,"fechaHora","id"))));
    }
    @Operation(summary = "Exportar historial a Excel, máximo 31 días")
    @GetMapping("/exportar") public ResponseEntity<StreamingResponseBody> exportar(@ModelAttribute RegistroFiltro filtro) {
        reportes.validarExportacion(filtro);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=registros.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(output->reportes.exportar(filtro,output));
    }
}
