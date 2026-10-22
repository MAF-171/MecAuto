package pe.mecauto.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pe.mecauto.dto.ReporteDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Exporta cualquier reporte a un archivo .xlsx con Apache POI. */
@Component
public class ExcelExporter {

    public byte[] exportar(ReporteDto reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte");

            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle titulo = libro.createCellStyle();
            Font grande = libro.createFont();
            grande.setBold(true);
            grande.setFontHeightInPoints((short) 14);
            titulo.setFont(grande);
            CellStyle encabezado = libro.createCellStyle();
            encabezado.setFont(negrita);
            encabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle dinero = libro.createCellStyle();
            dinero.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            int fila = 0;
            Row t = hoja.createRow(fila++);
            Cell ct = t.createCell(0);
            ct.setCellValue("MecAuto - " + reporte.titulo());
            ct.setCellStyle(titulo);
            hoja.createRow(fila++).createCell(0).setCellValue("Periodo: " + reporte.periodo());
            fila++;

            Row enc = hoja.createRow(fila++);
            List<String> columnas = reporte.columnas();
            for (int i = 0; i < columnas.size(); i++) {
                Cell c = enc.createCell(i);
                c.setCellValue(columnas.get(i));
                c.setCellStyle(encabezado);
            }
            for (List<Object> datos : reporte.filas()) {
                Row r = hoja.createRow(fila++);
                for (int i = 0; i < datos.size(); i++) {
                    escribir(r.createCell(i), datos.get(i), dinero);
                }
            }
            fila++;
            for (Map.Entry<String, Object> e : reporte.resumen().entrySet()) {
                Row r = hoja.createRow(fila++);
                Cell k = r.createCell(0);
                k.setCellValue(e.getKey());
                k.setCellStyle(encabezado);
                escribir(r.createCell(1), e.getValue(), dinero);
            }
            for (int i = 0; i < Math.max(columnas.size(), 2); i++) {
                hoja.autoSizeColumn(i);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel", e);
        }
    }

    private void escribir(Cell celda, Object valor, CellStyle dinero) {
        if (valor instanceof BigDecimal b) {
            celda.setCellValue(b.doubleValue());
            celda.setCellStyle(dinero);
        } else if (valor instanceof Number n) {
            celda.setCellValue(n.doubleValue());
        } else {
            // setCellValue(String) guarda texto: un valor que empiece con "=" no se ejecuta como fórmula
            celda.setCellValue(valor == null ? "" : valor.toString());
        }
    }
}
