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

        // Polices
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 42, Color.decode("#1f3b73"));
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Color.GRAY);
        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 40, Color.decode("#996515")); // Or plus sombre pour contraste
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 20, Color.BLACK);
        Font courseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.BLACK);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 14, Color.DARK_GRAY);

        // Contenu (Réduit les espacements pour tenir sur une page)
        Paragraph header = new Paragraph("CERTIFICAT DE REUSSITE", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingBefore(40);
        document.add(header);
        
        Paragraph subHeader = new Paragraph("Décerné par Skill Bridge", subTitleFont);
        subHeader.setAlignment(Element.ALIGN_CENTER);
        subHeader.setSpacingAfter(30);
        document.add(subHeader);

        Paragraph pTo = new Paragraph("Ce certificat est fièrement décerné à :", textFont);
        pTo.setAlignment(Element.ALIGN_CENTER);
        document.add(pTo);

        // Nom de l'étudiant
        String displayName = (studentName == null || studentName.trim().isEmpty()) ? "Étudiant" : studentName;
        Paragraph pName = new Paragraph(displayName, nameFont);
        pName.setAlignment(Element.ALIGN_CENTER);
        pName.setSpacingBefore(15);
        pName.setSpacingAfter(20);
        document.add(pName);

        Paragraph pSuccess = new Paragraph("Pour avoir validé avec succès le cours :", textFont);
        pSuccess.setAlignment(Element.ALIGN_CENTER);
        document.add(pSuccess);

        Paragraph pCourse = new Paragraph(courseName, courseFont);
        pCourse.setAlignment(Element.ALIGN_CENTER);
        pCourse.setSpacingBefore(5);
        pCourse.setSpacingAfter(20);
        document.add(pCourse);

        Paragraph pScore = new Paragraph("Score obtenu : " + score + "%", textFont);
        pScore.setAlignment(Element.ALIGN_CENTER);
        document.add(pScore);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Paragraph pDate = new Paragraph("\nDate de délivrance : " + sdf.format(new Date()), footerFont);
        pDate.setAlignment(Element.ALIGN_CENTER);
        document.add(pDate);

        document.close();
    }
}
