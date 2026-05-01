package com.pidev.Services;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Service de génération de certificat PDF optimisé pour une seule page.
 */
public class CertificateGeneratorService {

    public void generateCertificate(String destPath, String studentName, String courseName, int score) throws Exception {
        System.out.println("[CertificateGeneratorService] Generating for: '" + studentName + "'");
        // Rotation A4 Landscape pour un look certificat
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(destPath));
        
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                PdfContentByte canvas = writer.getDirectContentUnder();
                float width = document.getPageSize().getWidth();
                float height = document.getPageSize().getHeight();
                
                // Bordure élégante
                canvas.setLineWidth(8);
                canvas.setRGBColorStroke(31, 59, 115);
                canvas.rectangle(20, 20, width - 40, height - 40);
                canvas.stroke();
                
                canvas.setLineWidth(2);
                canvas.setRGBColorStroke(212, 175, 55);
                canvas.rectangle(35, 35, width - 70, height - 70);
                canvas.stroke();
                
                // Filigrane
                canvas.beginText();
                try {
                    canvas.setFontAndSize(com.lowagie.text.pdf.BaseFont.createFont(
                            com.lowagie.text.pdf.BaseFont.HELVETICA_BOLD, 
                            com.lowagie.text.pdf.BaseFont.WINANSI, false), 80);
                } catch (Exception e) {}
                canvas.setRGBColorFill(250, 250, 250);
                canvas.showTextAligned(Element.ALIGN_CENTER, "SKILL BRIDGE", width / 2, height / 2, 45);
                canvas.endText();
            }
        });

        document.open();

        // Polices aggrandies et colorées
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 54, Color.decode("#1e3a8a")); // Bleu Royal Profond
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 24, Color.decode("#64748b")); // Gris Ardoise
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 26, Color.decode("#334155")); // Ardoise Sombre
        Font courseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, Color.decode("#7c3aed")); // Violet Royal
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 18, Color.decode("#94a3b8")); // Gris Doux

        // Contenu (Réduit les espacements pour tenir sur une page)
        Paragraph header = new Paragraph("CERTIFICAT DE REUSSITE", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingBefore(40);
        document.add(header);
        
        Paragraph subHeader = new Paragraph("Décerné par Skill Bridge", subTitleFont);
        subHeader.setAlignment(Element.ALIGN_CENTER);
        subHeader.setSpacingAfter(30);
        document.add(subHeader);

        Paragraph pSuccess = new Paragraph("Attestation de validation avec succès du cours :", textFont);
        pSuccess.setAlignment(Element.ALIGN_CENTER);
        pSuccess.setSpacingBefore(40);
        document.add(pSuccess);

        Paragraph pCourse = new Paragraph(courseName, courseFont);
        pCourse.setAlignment(Element.ALIGN_CENTER);
        pCourse.setSpacingBefore(10);
        pCourse.setSpacingAfter(30);
        document.add(pCourse);

        Paragraph pScore = new Paragraph("Score obtenu : " + score + "%", textFont);
        pScore.setAlignment(Element.ALIGN_CENTER);
        document.add(pScore);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Paragraph pDate = new Paragraph("\nDate de validation : " + sdf.format(new Date()), footerFont);
        pDate.setAlignment(Element.ALIGN_CENTER);
        pDate.setSpacingBefore(20);
        document.add(pDate);

        document.close();
    }
}
