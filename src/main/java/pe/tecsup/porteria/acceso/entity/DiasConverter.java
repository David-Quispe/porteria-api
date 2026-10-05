package pe.tecsup.porteria.acceso.entity;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.*;
@Converter
public class DiasConverter implements AttributeConverter<Set<DayOfWeek>,String> {
    public String convertToDatabaseColumn(Set<DayOfWeek> dias) {
        return dias.stream().sorted().map(d -> Integer.toString(d.getValue())).collect(Collectors.joining(","));
    }
    public Set<DayOfWeek> convertToEntityAttribute(String dias) {
        Set<DayOfWeek> result=EnumSet.noneOf(DayOfWeek.class);
        for (String d : dias.split(",")) result.add(DayOfWeek.of(Integer.parseInt(d)));
        return result;
    }
}
