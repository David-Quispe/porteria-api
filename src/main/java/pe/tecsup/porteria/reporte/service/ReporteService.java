package pe.tecsup.porteria.reporte.service;
import java.io.*;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import pe.tecsup.porteria.acceso.dto.*;
import pe.tecsup.porteria.acceso.service.RegistroConsultaService;
import pe.tecsup.porteria.shared.exception.BusinessException;
@Service @RequiredArgsConstructor
public class ReporteService {
    private final RegistroConsultaService registros;
    public void validarExportacion(RegistroFiltro f) {
        RegistroConsultaService.validar(f);
        if(f.desde()==null || f.hasta()==null || Duration.between(f.desde(),f.hasta()).compareTo(Duration.ofDays(31))>0)
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,"La exportación requiere desde y hasta, con un máximo de 31 días");
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public void exportar(RegistroFiltro f,OutputStream output) throws IOException {
        validarExportacion(f);
        try(var workbook=new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            var sheet=workbook.createSheet("Accesos");
            String[] headers={"ID","Fecha y hora (Lima)","Dispositivo","Persona","Nombre","Método","Dirección","Resultado"};
            var head=sheet.createRow(0); for(int i=0;i<headers.length;i++) {head.createCell(i).setCellValue(headers[i]);sheet.setColumnWidth(i,24*256);}
            var style=workbook.createCellStyle();style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
            int fila=1,page=0; Page<RegistroResponse> data;
            do {
                data=registros.listar(f,PageRequest.of(page++,500,Sort.by("fechaHora","id")));
                for(var r:data) {
                    if(fila>=1_048_576) throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,"El reporte supera el límite de filas de Excel; reduzca el rango");
                    var row=sheet.createRow(fila++);
                    row.createCell(0).setCellValue(r.id().toString());
                    var date=row.createCell(1);date.setCellValue(r.fechaHora());date.setCellStyle(style);
                    row.createCell(2).setCellValue(r.dispositivoId().toString());
                    row.createCell(3).setCellValue(r.personaId()==null?"":r.personaId().toString());
                    row.createCell(4).setCellValue(r.nombre()==null?"":r.nombre());
                    row.createCell(5).setCellValue(r.metodo().name());
                    row.createCell(6).setCellValue(r.direccion().name());
                    row.createCell(7).setCellValue(r.resultado().name());
                }
            } while(data.hasNext());
            sheet.createFreezePane(0,1); workbook.write(output);
        }
    }
}
