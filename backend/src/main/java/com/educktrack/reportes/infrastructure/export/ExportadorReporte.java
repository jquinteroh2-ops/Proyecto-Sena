package com.educktrack.reportes.infrastructure.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Genera documentos exportables de reportes en PDF (OpenPDF) y Excel (Apache
 * POI) a partir de una tabla generica (RS-06, RF-49, RF-50).
 */
public final class ExportadorReporte {

    private ExportadorReporte() {
    }

    /** RF-49: exporta un reporte tabular a PDF. */
    public static byte[] aPdf(String titulo, List<String> encabezados, List<List<String>> filas) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph p = new Paragraph(titulo, tituloFont);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingAfter(16);
            doc.add(p);

            PdfPTable tabla = new PdfPTable(encabezados.size());
            tabla.setWidthPercentage(100);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            for (String h : encabezados) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setPadding(6);
                tabla.addCell(cell);
            }
            for (List<String> fila : filas) {
                for (String valor : fila) {
                    PdfPCell cell = new PdfPCell(new Phrase(valor));
                    cell.setPadding(5);
                    tabla.addCell(cell);
                }
            }
            doc.add(tabla);
            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF: " + ex.getMessage(), ex);
        }
    }

    /** RF-50: exporta un reporte tabular a Excel (.xlsx). */
    public static byte[] aExcel(String titulo, List<String> encabezados, List<List<String>> filas) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(titulo.length() > 31 ? titulo.substring(0, 31) : titulo);

            org.apache.poi.ss.usermodel.Font headFont = wb.createFont();
            headFont.setBold(true);
            CellStyle headStyle = wb.createCellStyle();
            headStyle.setFont(headFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < encabezados.size(); i++) {
                Cell c = header.createCell(i);
                c.setCellValue(encabezados.get(i));
                c.setCellStyle(headStyle);
            }
            int r = 1;
            for (List<String> fila : filas) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < fila.size(); i++) {
                    row.createCell(i).setCellValue(fila.get(i));
                }
            }
            for (int i = 0; i < encabezados.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el Excel: " + ex.getMessage(), ex);
        }
    }
}
