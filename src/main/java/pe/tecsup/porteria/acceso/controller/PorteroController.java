package pe.tecsup.porteria.acceso.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.acceso.dto.RegistroResponse;
import pe.tecsup.porteria.acceso.service.RegistroConsultaService;
import pe.tecsup.porteria.shared.dto.*;
@RestController @RequestMapping("/api/portero") @RequiredArgsConstructor
@Tag(name="Portería") @SecurityRequirement(name="jwt")
public class PorteroController {
    private final RegistroConsultaService service;
    @GetMapping("/ultima") public ResponseEntity<RegistroResponse> ultima() {
        return service.ultima().map(ResponseEntity::ok).orElseGet(()->ResponseEntity.noContent().build());
    }
    @GetMapping("/turno") public PageResponse<RegistroResponse> turno(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return PageResponse.of(service.turno(Paginacion.of(page,size,Sort.by(Sort.Direction.DESC,"fechaHora","id"))));
    }
}
