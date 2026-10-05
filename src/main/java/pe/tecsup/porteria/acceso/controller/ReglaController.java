package pe.tecsup.porteria.acceso.controller;
import java.net.URI;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.acceso.dto.*;
import pe.tecsup.porteria.acceso.service.ReglaService;
import pe.tecsup.porteria.shared.dto.*;
@RestController @RequestMapping("/api/admin/reglas") @RequiredArgsConstructor
@Tag(name="Reglas de acceso") @SecurityRequirement(name="jwt")
public class ReglaController {
    private final ReglaService service;
    @Operation(summary = "Listar reglas de acceso")
    @GetMapping public PageResponse<ReglaResponse> listar(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.listar(page,size); }
    @Operation(summary = "Obtener regla de acceso")
    @GetMapping("/{id}") public ReglaResponse obtener(@PathVariable Long id) { return service.obtener(id); }
    @Operation(summary = "Crear regla de acceso")
    @PostMapping public ResponseEntity<ReglaResponse> crear(@Valid @RequestBody ReglaRequest r) {
        var result=service.guardar(null,r); return ResponseEntity.created(URI.create("/api/admin/reglas/"+result.id())).body(result);
    }
    @Operation(summary = "Actualizar regla de acceso")
    @PutMapping("/{id}") public ReglaResponse editar(@PathVariable Long id,@Valid @RequestBody ReglaRequest r) { return service.guardar(id,r); }
    @Operation(summary = "Cambiar estado de regla")
    @PatchMapping("/{id}/estado") public ResponseEntity<Void> estado(@PathVariable Long id,@Valid @RequestBody EstadoRequest r) { service.estado(id,r.activo());return ResponseEntity.noContent().build(); }
    @Operation(summary = "Desactivar regla")
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { service.estado(id,false);return ResponseEntity.noContent().build(); }
}
