package pe.tecsup.porteria.acceso.dto;
import pe.tecsup.porteria.acceso.entity.Resultado;
public record LecturaResponse(Resultado resultado,boolean abrir,String nombre) {}
