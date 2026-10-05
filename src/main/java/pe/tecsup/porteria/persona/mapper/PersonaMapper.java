package pe.tecsup.porteria.persona.mapper;
import org.mapstruct.*;
import pe.tecsup.porteria.persona.dto.*;
import pe.tecsup.porteria.persona.entity.Persona;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PersonaMapper {
    PersonaResponse toResponse(Persona persona);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fotoUrl", ignore = true)
    void update(PersonaRequest request, @MappingTarget Persona persona);
}
