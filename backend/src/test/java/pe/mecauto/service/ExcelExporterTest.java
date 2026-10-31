package pe.mecauto.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.mecauto.dto.ReporteDto;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExporterTest {

    private final ExcelExporter exportador = new ExcelExporter();

    private ReporteDto reporteDeEjemplo() {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Órdenes", 2);
        resumen.put("Monto total (S/)", new BigDecimal("350.50"));
        return new ReporteDto("ordenes", "Órdenes de trabajo", "01/10/2026 al 31/10/2026",
                List.of("Código", "Cliente", "Total (S/)"),
                List.of(List.of("OT-0001", "=SUM(1+1)", new BigDecimal("120.50")),
                        List.of("OT-0002", "María Torres", new BigDecimal("230.00"))),
                resumen);
    }

    private XSSFWorkbook abrir(byte[] bytes) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    @Test
    @DisplayName("El Excel lleva el logo de MecAuto, el título y el encabezado de la tabla")
    void incluyeLogoTituloYEncabezado() throws Exception {
        try (XSSFWorkbook libro = abrir(exportador.exportar(reporteDeEjemplo()))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals(1, libro.getAllPictures().size(), "debe incluir el logo");
            XSSFDrawing dibujo = ((XSSFSheet) hoja).getDrawingPatriarch();
            assertNotNull(dibujo, "la hoja debe tener una capa de dibujo con el logo");
            assertEquals(1, dibujo.getShapes().size(), "el logo debe estar dibujado en la hoja");
            assertTrue(dibujo.getShapes().get(0) instanceof XSSFPicture);
            assertEquals("Órdenes de trabajo", hoja.getRow(4).getCell(0).getStringCellValue());
            assertTrue(hoja.getRow(5).getCell(0).getStringCellValue().startsWith("Periodo: 01/10/2026"));
            assertEquals("Código", hoja.getRow(8).getCell(0).getStringCellValue());
            assertEquals("Total (S/)", hoja.getRow(8).getCell(2).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Los importes salen como números y un texto que empieza con '=' no se ejecuta como fórmula")
    void importesNumericosYTextoSeguro() throws Exception {
        try (XSSFWorkbook libro = abrir(exportador.exportar(reporteDeEjemplo()))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals(CellType.NUMERIC, hoja.getRow(9).getCell(2).getCellType());
            assertEquals(120.50, hoja.getRow(9).getCell(2).getNumericCellValue(), 0.001);
            assertEquals(CellType.STRING, hoja.getRow(9).getCell(1).getCellType());
            assertEquals("=SUM(1+1)", hoja.getRow(9).getCell(1).getStringCellValue());
        }
    }

    @Test
    @DisplayName("El resumen se escribe debajo de la tabla")
    void incluyeResumen() throws Exception {
        try (XSSFWorkbook libro = abrir(exportador.exportar(reporteDeEjemplo()))) {
            Sheet hoja = libro.getSheetAt(0);
            // filas de datos: 9 y 10; luego una en blanco (11), "Resumen" (12), y sus dos filas
            assertEquals("Resumen", hoja.getRow(12).getCell(0).getStringCellValue());
            assertEquals("Órdenes", hoja.getRow(13).getCell(0).getStringCellValue());
            assertEquals(2.0, hoja.getRow(13).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(350.50, hoja.getRow(14).getCell(1).getNumericCellValue(), 0.001);
        }
    }
}
