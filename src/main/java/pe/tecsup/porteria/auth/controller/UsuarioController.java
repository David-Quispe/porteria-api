package pe.tecsup.porteria.auth.controller;
import java.net.URI;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.auth.dto.*;
import pe.tecsup.porteria.auth.service.UsuarioAdminService;
import pe.tecsup.porteria.shared.dto.*;
@RestController @RequestMapping("/api/admin/usuarios") @RequiredArgsConstructor
@Tag(name="Usuarios") @SecurityRequirement(name="jwt")
public class UsuarioController {
    private final UsuarioAdminService service;
    @Operation(summary = "Listar usuarios")
    @GetMapping public PageResponse<UsuarioResponse> listar(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return service.listar(page,size);}
    @Operation(summary = "Obtener usuario")
    @GetMapping("/{id}") public UsuarioResponse obtener(@PathVariable Long id) {return service.obtener(id);}
    @Operation(summary = "Crear usuario")
    @PostMapping public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest r) {
        var result=service.crear(r);return ResponseEntity.created(URI.create("/api/admin/usuarios/"+result.id())).body(result);
    }
    @Operation(summary = "Actualizar usuario y rol")
    @PutMapping("/{id}") public UsuarioResponse editar(@PathVariable Long id,@Valid @RequestBody UsuarioUpdate r) {return service.editar(id,r);}
    @Operation(summary = "Cambiar estado de usuario")
    @PatchMapping("/{id}/estado") public ResponseEntity<Void> estado(@PathVariable Long id,@Valid @RequestBody EstadoRequest r) {service.estado(id,r.activo());return ResponseEntity.noContent().build();}
    @Operation(summary = "Desactivar usuario")
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) {service.estado(id,false);return ResponseEntity.noContent().build();}
    @Operation(summary = "Cambiar contraseña y revocar sesiones")
    @PutMapping("/{id}/password") public ResponseEntity<Void> password(@PathVariable Long id,@Valid @RequestBody PasswordRequest r) {service.password(id,r.password());return ResponseEntity.noContent().build();}
}
