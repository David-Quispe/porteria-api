package pe.tecsup.porteria.persona.controller;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.persona.dto.*;
import pe.tecsup.porteria.persona.entity.TipoPersona;
import pe.tecsup.porteria.persona.service.*;
import pe.tecsup.porteria.shared.dto.*;
@RestController
@RequestMapping("/api/admin/personas")
@RequiredArgsConstructor
@Tag(name = "Personas y credenciales")
@SecurityRequirement(name = "jwt")
public class PersonaController {
    private final PersonaAdminService personas;
    private final CredencialAdminService credenciales;
    @GetMapping
    public PageResponse<PersonaResponse> listar(@RequestParam(required=false) String q,
            @RequestParam(required=false) TipoPersona tipo, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return personas.listar(q, tipo, page, size); }
    @GetMapping("/{id}")
    public PersonaResponse obtener(@PathVariable Long id) { return personas.obtener(id); }
    @PostMapping
    public ResponseEntity<PersonaResponse> crear(@Valid @RequestBody PersonaRequest request) {
        var response = personas.guardar(null, request);
        return ResponseEntity.created(URI.create("/api/admin/personas/" + response.id())).body(response);
    }
    @PutMapping("/{id}")
    public PersonaResponse editar(@PathVariable Long id, @Valid @RequestBody PersonaRequest request) {
        return personas.guardar(id, request);
    }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        personas.estado(id, request.activo()); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        personas.estado(id, false); return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/credenciales")
    public List<CredencialResponse> credenciales(@PathVariable Long id) { return credenciales.listar(id); }
    @PostMapping("/{id}/credenciales")
    public ResponseEntity<CredencialResponse> agregar(@PathVariable Long id, @Valid @RequestBody CredencialRequest request) {
        var response = credenciales.crear(id, request);
        return ResponseEntity.created(URI.create("/api/admin/personas/" + id + "/credenciales/" + response.id())).body(response);
    }
    @PatchMapping("/{id}/credenciales/{credencialId}/estado")
    public ResponseEntity<Void> estadoCredencial(@PathVariable Long id, @PathVariable Long credencialId,
            @Valid @RequestBody EstadoRequest request) {
        credenciales.estado(id, credencialId, request.activo()); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}/credenciales/{credencialId}")
    public ResponseEntity<Void> eliminarCredencial(@PathVariable Long id, @PathVariable Long credencialId) {
        credenciales.eliminar(id, credencialId); return ResponseEntity.noContent().build();
    }
}
