package pe.mecauto.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pe.mecauto.dto.ReporteDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exporta cualquier reporte a un archivo .xlsx con Apache POI.
 * El archivo lleva la identidad de MecAuto: franja negra con el logo, encabezado rojo, filas alternadas,
 * filtros, panel inmovilizado y configuración de impresión (horizontal, ajustado al ancho de la página).
 */
@Component
public class ExcelExporter {

    private static final Logger log = LoggerFactory.getLogger(ExcelExporter.class);
    private static final String RUTA_LOGO = "branding/logo-reporte.png";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    // Paleta de la marca
    private static final byte[] NEGRO = rgb(0x0B, 0x0B, 0x0B);
    private static final byte[] ROJO = rgb(0xE5, 0x09, 0x14);
    private static final byte[] BLANCO = rgb(0xFF, 0xFF, 0xFF);
    private static final byte[] GRIS_FILA = rgb(0xF3, 0xF3, 0xF3);
    private static final byte[] GRIS_RESUMEN = rgb(0xE4, 0xE4, 0xE4);
    private static final byte[] GRIS_BORDE = rgb(0xBF, 0xBF, 0xBF);
    private static final byte[] GRIS_TEXTO = rgb(0x59, 0x59, 0x59);

    /** Columnas mínimas que cubre la franja del logo, para que se vea completa aunque el reporte tenga pocas columnas. */
    private static final int COLUMNAS_FRANJA_MINIMA = 6;
    /** Alto de la franja negra con el logo (filas 1 y 2: 28 pt cada una). */
    private static final double ALTO_FRANJA_PT = 56;
    private static final int EMU_POR_PIXEL = 9525;

    public byte[] exportar(ReporteDto reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte");
            hoja.setDisplayGridlines(false);
            libro.getProperties().getCoreProperties().setTitle("MecAuto - " + reporte.titulo());
            libro.getProperties().getCoreProperties().setCreator("MecAuto");

            List<String> columnas = reporte.columnas();
            int nCols = Math.max(columnas.size(), 1);
            int colsFranja = Math.max(nCols, COLUMNAS_FRANJA_MINIMA);

            Estilos est = new Estilos(libro);

            // ---- Franja de marca (filas 0 a 2): negra con el logo y una línea roja
            alturaFila(hoja, 0, 28);
            alturaFila(hoja, 1, 28);
            alturaFila(hoja, 2, 4);
            for (int f = 0; f < 3; f++) {
                Row r = hoja.getRow(f);
                for (int c = 0; c < colsFranja; c++) {
                    r.createCell(c).setCellStyle(f < 2 ? est.franjaNegra : est.franjaRoja);
                }
            }
            alturaFila(hoja, 3, 8);

            // ---- Título, periodo y fecha de generación (combinados para no ensanchar la columna A)
            int fila = 4;
            Row t = hoja.createRow(fila);
            t.setHeightInPoints(26);
            celdaCombinada(hoja, t, fila, nCols, reporte.titulo(), est.titulo);
            fila++;
            celdaCombinada(hoja, hoja.createRow(fila), fila, nCols, "Periodo: " + reporte.periodo(), est.subtitulo);
            fila++;
            celdaCombinada(hoja, hoja.createRow(fila), fila, nCols,
                    "Generado el " + LocalDateTime.now(ZONA).format(FORMATO_FECHA), est.subtitulo);
            fila++;
            alturaFila(hoja, fila++, 8);

            // ---- Tabla
            int filaEncabezado = fila;
            Row enc = hoja.createRow(fila++);
            enc.setHeightInPoints(22);
            for (int i = 0; i < columnas.size(); i++) {
                Cell c = enc.createCell(i);
                c.setCellValue(columnas.get(i));
                c.setCellStyle(est.encabezado);
            }
            int n = 0;
            for (List<Object> datos : reporte.filas()) {
                Row r = hoja.createRow(fila++);
                boolean alterna = n++ % 2 == 1;
                for (int i = 0; i < datos.size(); i++) {
                    escribir(r.createCell(i), datos.get(i), est, alterna);
                }
            }
            int ultimaFilaTabla = fila - 1;
            if (!columnas.isEmpty()) {
                hoja.setAutoFilter(new CellRangeAddress(filaEncabezado, ultimaFilaTabla, 0, columnas.size() - 1));
            }

            // ---- Resumen
            if (reporte.resumen() != null && !reporte.resumen().isEmpty()) {
                fila++;
                Row rr = hoja.createRow(fila++);
                Cell titRes = rr.createCell(0);
                titRes.setCellValue("Resumen");
                titRes.setCellStyle(est.tituloResumen);
                for (Map.Entry<String, Object> e : reporte.resumen().entrySet()) {
                    Row r = hoja.createRow(fila++);
                    Cell k = r.createCell(0);
                    k.setCellValue(e.getKey());
                    k.setCellStyle(est.etiquetaResumen);
                    escribirResumen(r.createCell(1), e.getValue(), est, esTotal(e.getKey()));
                }
            }

            // ---- Pie del documento
            fila++;
            celdaCombinada(hoja, hoja.createRow(fila), fila, nCols,
                    "Documento generado por MecAuto - Sistema de gestión del taller. Uso interno.", est.pie);

            // ---- Anchos de columna (antes de colocar el logo, que se dimensiona con ellos)
            for (int i = 0; i < colsFranja; i++) {
                if (i < nCols) {
                    hoja.autoSizeColumn(i);
                    int ancho = hoja.getColumnWidth(i) + 2 * 256;
                    if (i < columnas.size()) {
                        // el encabezado va en negrita y lleva la flecha del filtro: necesita más espacio
                        ancho = Math.max(ancho, (int) ((columnas.get(i).length() * 1.2 + 5) * 256));
                    }
                    hoja.setColumnWidth(i, Math.min(Math.max(ancho, 12 * 256), 42 * 256));
                } else {
                    hoja.setColumnWidth(i, 14 * 256);
                }
            }

            colocarLogo(libro, hoja);
            configurarImpresion(hoja, filaEncabezado);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel", e);
        }
    }

    // ------------------------------------------------------------------ logo e impresión

    /**
     * Inserta el logo en la franja negra (filas 1 y 2, esquina izquierda). Usa un ancla explícita calculada
     * con los anchos reales de las columnas. Si el archivo no existe, el reporte se genera sin logo.
     */
    private void colocarLogo(XSSFWorkbook libro, Sheet hoja) {
        try (InputStream in = new ClassPathResource(RUTA_LOGO).getInputStream()) {
            int idx = libro.addPicture(in.readAllBytes(), Workbook.PICTURE_TYPE_PNG);

            // La franja mide ALTO_FRANJA_PT; la imagen es 4 veces más ancha que alta (600 x 150 px)
            double altoPx = ALTO_FRANJA_PT * 96.0 / 72.0;
            double restante = altoPx * 4.0;
            int col = 0;
            while (col < 50) {
                double anchoCol = hoja.getColumnWidthInPixels(col);
                if (restante <= anchoCol) {
                    break;
                }
                restante -= anchoCol;
                col++;
            }
            int dx2 = (int) (restante * EMU_POR_PIXEL);
            // (dx1, dy1, dx2, dy2, col1, row1, col2, row2): de la celda A1 hasta el inicio de la fila 3
            XSSFClientAnchor ancla = new XSSFClientAnchor(0, 0, dx2, 0, 0, 0, col, 2);
            ancla.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
            hoja.createDrawingPatriarch().createPicture(ancla, idx);
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo insertar el logo en el reporte Excel: {}", e.toString(), e);
        }
    }

    private void configurarImpresion(Sheet hoja, int filaEncabezado) {
        PrintSetup ps = hoja.getPrintSetup();
        ps.setLandscape(true);
        ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
        ps.setFitWidth((short) 1);
        ps.setFitHeight((short) 0); // todas las filas que hagan falta
        hoja.setFitToPage(true);
        hoja.setHorizontallyCenter(true);
        hoja.setRepeatingRows(new CellRangeAddress(filaEncabezado, filaEncabezado, -1, -1));
        hoja.getFooter().setLeft("MecAuto");
        hoja.getFooter().setRight("Página &P de &N");
    }

    // ------------------------------------------------------------------ celdas

    private void escribir(Cell celda, Object valor, Estilos est, boolean alterna) {
        if (valor instanceof BigDecimal || valor instanceof Double || valor instanceof Float) {
            celda.setCellValue(((Number) valor).doubleValue());
            celda.setCellStyle(alterna ? est.dineroAlterno : est.dinero);
        } else if (valor instanceof Number n) {
            celda.setCellValue(n.doubleValue());
            celda.setCellStyle(alterna ? est.enteroAlterno : est.entero);
        } else {
            // setCellValue(String) guarda texto: un valor que empiece con "=" no se ejecuta como fórmula
            celda.setCellValue(valor == null ? "" : valor.toString());
            celda.setCellStyle(alterna ? est.textoAlterno : est.texto);
        }
    }

    private void escribirResumen(Cell celda, Object valor, Estilos est, boolean destacado) {
        if (valor instanceof BigDecimal || valor instanceof Double || valor instanceof Float) {
            celda.setCellValue(((Number) valor).doubleValue());
            celda.setCellStyle(destacado ? est.resumenDineroDestacado : est.resumenDinero);
        } else if (valor instanceof Number n) {
            celda.setCellValue(n.doubleValue());
            celda.setCellStyle(destacado ? est.resumenEnteroDestacado : est.resumenEntero);
        } else {
            celda.setCellValue(valor == null ? "" : valor.toString());
            celda.setCellStyle(est.resumenTexto);
        }
    }

    private static boolean esTotal(String etiqueta) {
        String s = etiqueta == null ? "" : etiqueta.toLowerCase();
        return s.contains("total") || s.contains("monto") || s.contains("ingreso");
    }

    private void celdaCombinada(Sheet hoja, Row fila, int indice, int nCols, String texto, XSSFCellStyle estilo) {
        for (int c = 0; c < nCols; c++) {
            fila.createCell(c).setCellStyle(estilo);
        }
        fila.getCell(0).setCellValue(texto);
        if (nCols > 1) {
            hoja.addMergedRegion(new CellRangeAddress(indice, indice, 0, nCols - 1));
        }
    }

    private void alturaFila(Sheet hoja, int indice, float puntos) {
        Row r = hoja.getRow(indice) != null ? hoja.getRow(indice) : hoja.createRow(indice);
        r.setHeightInPoints(puntos);
    }

    private static byte[] rgb(int r, int g, int b) {
        return new byte[]{(byte) r, (byte) g, (byte) b};
    }

    // ------------------------------------------------------------------ estilos

    /** Todos los estilos del libro, creados una sola vez (Excel limita la cantidad de estilos por archivo). */
    private static final class Estilos {
        final XSSFCellStyle franjaNegra, franjaRoja, titulo, subtitulo, encabezado, pie;
        final XSSFCellStyle texto, textoAlterno, entero, enteroAlterno, dinero, dineroAlterno;
        final XSSFCellStyle tituloResumen, etiquetaResumen, resumenTexto;
        final XSSFCellStyle resumenEntero, resumenDinero, resumenEnteroDestacado, resumenDineroDestacado;

        Estilos(XSSFWorkbook libro) {
            short fmtEntero = libro.createDataFormat().getFormat("#,##0");
            short fmtDinero = libro.createDataFormat().getFormat("#,##0.00");

            franjaNegra = base(libro, NEGRO);
            franjaRoja = base(libro, ROJO);

            titulo = sinRelleno(libro, fuente(libro, 16, true, NEGRO), HorizontalAlignment.LEFT);
            subtitulo = sinRelleno(libro, fuente(libro, 10, false, GRIS_TEXTO), HorizontalAlignment.LEFT);
            pie = sinRelleno(libro, fuente(libro, 8, false, GRIS_TEXTO), HorizontalAlignment.LEFT);

            encabezado = base(libro, ROJO);
            encabezado.setFont(fuente(libro, 11, true, BLANCO));
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            encabezado.setWrapText(true);
            bordes(encabezado, GRIS_BORDE);

            XSSFFont normal = fuente(libro, 10, false, NEGRO);
            texto = dato(libro, normal, null, HorizontalAlignment.LEFT, (short) 0);
            textoAlterno = dato(libro, normal, GRIS_FILA, HorizontalAlignment.LEFT, (short) 0);
            entero = dato(libro, normal, null, HorizontalAlignment.RIGHT, fmtEntero);
            enteroAlterno = dato(libro, normal, GRIS_FILA, HorizontalAlignment.RIGHT, fmtEntero);
            dinero = dato(libro, normal, null, HorizontalAlignment.RIGHT, fmtDinero);
            dineroAlterno = dato(libro, normal, GRIS_FILA, HorizontalAlignment.RIGHT, fmtDinero);

            tituloResumen = sinRelleno(libro, fuente(libro, 12, true, NEGRO), HorizontalAlignment.LEFT);
            tituloResumen.setBorderBottom(BorderStyle.MEDIUM);
            tituloResumen.setBottomBorderColor(new XSSFColor(ROJO, null));

            XSSFFont negrita = fuente(libro, 10, true, NEGRO);
            XSSFFont rojoNegrita = fuente(libro, 11, true, ROJO);
            etiquetaResumen = dato(libro, negrita, GRIS_RESUMEN, HorizontalAlignment.LEFT, (short) 0);
            resumenTexto = dato(libro, negrita, null, HorizontalAlignment.RIGHT, (short) 0);
            resumenEntero = dato(libro, negrita, null, HorizontalAlignment.RIGHT, fmtEntero);
            resumenDinero = dato(libro, negrita, null, HorizontalAlignment.RIGHT, fmtDinero);
            resumenEnteroDestacado = dato(libro, rojoNegrita, null, HorizontalAlignment.RIGHT, fmtEntero);
            resumenDineroDestacado = dato(libro, rojoNegrita, null, HorizontalAlignment.RIGHT, fmtDinero);
        }

        private static XSSFFont fuente(XSSFWorkbook libro, int puntos, boolean negrita, byte[] color) {
            XSSFFont f = libro.createFont();
            f.setFontName("Calibri");
            f.setFontHeightInPoints((short) puntos);
            f.setBold(negrita);
            f.setColor(new XSSFColor(color, null));
            return f;
        }

        private static XSSFCellStyle base(XSSFWorkbook libro, byte[] relleno) {
            XSSFCellStyle s = libro.createCellStyle();
            s.setFillForegroundColor(new XSSFColor(relleno, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }

        private static XSSFCellStyle sinRelleno(XSSFWorkbook libro, XSSFFont fuente, HorizontalAlignment h) {
            XSSFCellStyle s = libro.createCellStyle();
            s.setFont(fuente);
            s.setAlignment(h);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }

        private static XSSFCellStyle dato(XSSFWorkbook libro, XSSFFont fuente, byte[] relleno,
                                          HorizontalAlignment h, short formato) {
            XSSFCellStyle s = libro.createCellStyle();
            s.setFont(fuente);
            s.setAlignment(h);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            if (relleno != null) {
                s.setFillForegroundColor(new XSSFColor(relleno, null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (formato != 0) {
                s.setDataFormat(formato);
            }
            bordes(s, GRIS_BORDE);
            return s;
        }

        private static void bordes(XSSFCellStyle s, byte[] color) {
            XSSFColor c = new XSSFColor(color, null);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            s.setTopBorderColor(c);
            s.setBottomBorderColor(c);
            s.setLeftBorderColor(c);
            s.setRightBorderColor(c);
        }
    }
}
