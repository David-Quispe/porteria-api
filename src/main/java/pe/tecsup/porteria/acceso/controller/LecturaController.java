package pe.tecsup.porteria.acceso.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.acceso.dto.*;
import pe.tecsup.porteria.acceso.service.AccesoService;
import pe.tecsup.porteria.dispositivo.security.DispositivoAutenticado;
@RestController @RequiredArgsConstructor @Tag(name="ESP32") @SecurityRequirement(name="deviceToken")
public class LecturaController {
    private final AccesoService service;
    @PostMapping("/api/dispositivo/lecturas")
    public LecturaResponse leer(@AuthenticationPrincipal DispositivoAutenticado dispositivo,@Valid @RequestBody LecturaRequest request) {
        return service.registrar(dispositivo,request);
    }
}
