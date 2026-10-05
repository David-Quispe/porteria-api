package pe.tecsup.porteria.dispositivo.controller;
import java.net.URI;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.dispositivo.dto.*;
import pe.tecsup.porteria.dispositivo.service.DispositivoService;
import pe.tecsup.porteria.shared.dto.*;
@RestController
@RequestMapping("/api/admin/dispositivos")
@RequiredArgsConstructor
@Tag(name="Dispositivos")
@SecurityRequirement(name="jwt")
public class DispositivoAdminController {
    private final DispositivoService service;
    @GetMapping public PageResponse<DispositivoResponse> listar(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return service.listar(page,size); }
    @GetMapping("/{id}") public DispositivoResponse obtener(@PathVariable Long id) { return service.obtener(id); }
    @PostMapping public ResponseEntity<TokenResponse> crear(@Valid @RequestBody DispositivoRequest r) {
        var d=service.crear(r);
        return ResponseEntity.created(URI.create("/api/admin/dispositivos/"+d.dispositivo().id())).cacheControl(CacheControl.noStore()).body(d);
    }
    @PutMapping("/{id}") public DispositivoResponse editar(@PathVariable Long id,@Valid @RequestBody DispositivoRequest r) {
        return service.editar(id,r);
    }
    @PatchMapping("/{id}/estado") public ResponseEntity<Void> estado(@PathVariable Long id,@Valid @RequestBody EstadoRequest r) {
        service.estado(id,r.activo()); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.estado(id,false); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/token") public ResponseEntity<TokenResponse> regenerar(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.regenerar(id));
    }
}
