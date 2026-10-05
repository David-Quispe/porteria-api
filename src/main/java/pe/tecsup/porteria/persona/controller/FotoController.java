package pe.tecsup.porteria.persona.controller;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.persona.service.FotoService;
@RestController @RequiredArgsConstructor @Tag(name="Fotografías") @SecurityRequirement(name="jwt")
public class FotoController {
    private final FotoService service;
    @PostMapping(value="/api/admin/personas/{id}/foto",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,String> guardar(@PathVariable Long id,@RequestPart("file") MultipartFile file) {
        return Map.of("fotoUrl",service.guardar(id,file));
    }
    @GetMapping("/fotos/{nombre}")
    public ResponseEntity<Resource> obtener(@PathVariable String nombre) {
        var archivo=service.ruta(nombre);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(nombre.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG)
                .body(new FileSystemResource(archivo));
    }
}
