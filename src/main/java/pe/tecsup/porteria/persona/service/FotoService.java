package pe.tecsup.porteria.persona.service;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import pe.tecsup.porteria.persona.repository.PersonaRepository;
import pe.tecsup.porteria.shared.exception.*;
import lombok.extern.slf4j.Slf4j;
@Service @Slf4j
public class FotoService {
    private final Path directorio;
    private final PersonaRepository personas;
    public FotoService(PersonaRepository personas,@Value("${porteria.fotos.directorio:data/fotos}") String directorio) {
        this.personas=personas;this.directorio=Path.of(directorio).toAbsolutePath().normalize();
    }
    @Transactional
    public String guardar(Long id,MultipartFile file) {
        var persona=personas.findById(id).orElseThrow(()->new NotFoundException("Persona",id));
        if(file.isEmpty() || file.getSize()>2*1024*1024) throw invalida("La foto debe pesar entre 1 byte y 2 MB");
        Path nueva=null;
        try(var input=ImageIO.createImageInputStream(file.getInputStream())) {
            if(input==null) throw invalida("Imagen inválida");
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw invalida("Solo se permiten imágenes JPG y PNG válidas");
            var reader=readers.next();
            try {
                reader.setInput(input);
                String formato=reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if(!formato.equals("png") && !formato.equals("jpeg") && !formato.equals("jpg")) throw invalida("Solo se permiten JPG y PNG");
                if((long)reader.getWidth(0)*reader.getHeight(0)>16_000_000) throw invalida("La imagen supera 16 megapíxeles");
                var image=reader.read(0);
                Files.createDirectories(directorio);
                String extension=formato.equals("png")?"png":"jpg";
                nueva=directorio.resolve(UUID.randomUUID()+"."+extension);
                if(!ImageIO.write(image,extension,nueva.toFile())) throw invalida("No se pudo procesar la imagen");
                String anterior=persona.getFotoUrl();
                Path archivo=nueva;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if(status!=STATUS_COMMITTED) borrar(archivo);
                        else if(anterior!=null && anterior.startsWith("/fotos/")) {
                            try { borrar(ruta(anterior.substring(7))); } catch(NotFoundException ignored) { }
                        }
                    }
                });
                persona.setFotoUrl("/fotos/"+nueva.getFileName());
                personas.flush();
                return persona.getFotoUrl();
            } finally { reader.dispose(); }
        } catch(IOException e) {
            if(nueva!=null) borrar(nueva);
            throw new BusinessException(HttpStatus.BAD_REQUEST,"No se pudo leer o almacenar la imagen");
        } catch(RuntimeException e) {
            if(nueva!=null) borrar(nueva);
            throw e;
        }
    }
    public Path ruta(String nombre) {
        if(!nombre.matches("[0-9a-f-]{36}\\.(png|jpg)")) throw new NotFoundException("Foto no encontrada");
        Path archivo=directorio.resolve(nombre).normalize();
        if(!archivo.startsWith(directorio) || !Files.isRegularFile(archivo,LinkOption.NOFOLLOW_LINKS))
            throw new NotFoundException("Foto no encontrada");
        return archivo;
    }
    private void borrar(Path archivo) {
        try { Files.deleteIfExists(archivo); } catch(IOException e) { log.warn("No se pudo limpiar una foto almacenada"); }
    }
    private BusinessException invalida(String message) { return new BusinessException(HttpStatus.BAD_REQUEST,message); }
}
