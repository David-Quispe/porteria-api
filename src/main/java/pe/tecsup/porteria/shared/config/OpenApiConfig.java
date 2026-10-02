package pe.tecsup.porteria.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger en /swagger-ui.html. Cada controlador declara qué esquema usa:
 * {@code @SecurityRequirement(name = OpenApiConfig.JWT)} o {@code @SecurityRequirement(name = OpenApiConfig.DEVICE_TOKEN)}.
 */
@Configuration
public class OpenApiConfig {

    public static final String JWT = "jwt";
    public static final String DEVICE_TOKEN = "deviceToken";

    @Bean
    OpenAPI porteriaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Portería")
                        .version("v1")
                        .description("Identificación en portería con sticker NFC, código QR o DNI."))
                .components(new Components()
                        .addSecuritySchemes(JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /api/auth/login"))
                        .addSecuritySchemes(DEVICE_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Device-Token")
                                .description("Token del ESP32, entregado una sola vez al registrar el dispositivo")));
    }
}
