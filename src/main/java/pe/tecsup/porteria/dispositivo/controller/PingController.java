package pe.tecsup.porteria.dispositivo.controller;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import pe.tecsup.porteria.dispositivo.security.DispositivoAutenticado;
import pe.tecsup.porteria.dispositivo.service.DispositivoService;
@RestController
@RequiredArgsConstructor
@Tag(name="ESP32")
@SecurityRequirement(name="deviceToken")
public class PingController {
    private final DispositivoService service;
    public record PingResponse(boolean ok,LocalDateTime hora) {}
    @Operation(summary = "Registrar señal de vida del dispositivo")
    @PostMapping("/api/dispositivo/ping") public PingResponse ping(@AuthenticationPrincipal DispositivoAutenticado dispositivo) {
        return new PingResponse(true,service.ping(dispositivo));
    }
}
