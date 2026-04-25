package com.pidev.Services;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CertificateGeneratorService {

    public void generateCertificate(String destPath, String studentName, String courseName, int score) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(destPath));
        
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                PdfContentByte canvas = writer.getDirectContentUnder();
                
                canvas.setLineWidth(5);
                canvas.setRGBColorStroke(200, 160, 50); 
                canvas.rectangle(20, 20, document.getPageSize().getWidth() - 40, document.getPageSize().getHeight() - 40);
                canvas.stroke();
                
                canvas.beginText();
                try {
                    canvas.setFontAndSize(com.lowagie.text.pdf.BaseFont.createFont(), 100);
                } catch (Exception e) {}
                canvas.setRGBColorFill(240, 240, 240);
                canvas.showTextAligned(Element.ALIGN_CENTER, "SKILL BRIDGE", 
                        document.getPageSize().getWidth() / 2, 
                        document.getPageSize().getHeight() / 2, 45);
                canvas.endText();
            }
        });

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 40, Color.decode("#1f3b73"));
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 24, Color.DARK_GRAY);
        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32, Color.decode("#d4af37")); 
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Color.BLACK);

        Paragraph header = new Paragraph("CERTIFICAT DE REUSSITE", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingBefore(50);
        document.add(header);
        
        Paragraph subHeader = new Paragraph("Décerné par Skill Bridge", subTitleFont);
        subHeader.setAlignment(Element.ALIGN_CENTER);
        subHeader.setSpacingAfter(40);
        document.add(subHeader);

        Paragraph namePara = new Paragraph(studentName != null && !studentName.isBlank() ? studentName : "Étudiant Performant", nameFont);
        namePara.setAlignment(Element.ALIGN_CENTER);
        namePara.setSpacingAfter(20);
        document.add(namePara);

        String customPhrase = "A démontré sa maîtrise du cours " + courseName + " avec une note de " + score + "%";
        Paragraph textPara = new Paragraph(customPhrase, textFont);
        textPara.setAlignment(Element.ALIGN_CENTER);
        textPara.setSpacingAfter(40);
        document.add(textPara);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Paragraph dateScore = new Paragraph("Date de validation : " + sdf.format(new Date()) + "\nScore final : " + score + "%", textFont);
        dateScore.setAlignment(Element.ALIGN_CENTER);
        dateScore.setSpacingAfter(40);
        document.add(dateScore);

        PdfContentByte cb = writer.getDirectContent();
        
        String verifyUrl = "https://skillbridge.com/verify/" + System.currentTimeMillis();
        Image qrImage = Image.getInstance(generateQRCode(verifyUrl));
        qrImage.setAbsolutePosition(document.getPageSize().getWidth() - 180, 50);
        qrImage.scalePercent(80);
        document.add(qrImage);
        
        cb.beginText();
        try {
            cb.setFontAndSize(com.lowagie.text.pdf.BaseFont.createFont(), 14);
            cb.setRGBColorFill(0, 0, 0);
            cb.showTextAligned(Element.ALIGN_CENTER, "Signature du formateur", 150, 100, 0);
            cb.showTextAligned(Element.ALIGN_CENTER, "_____________________", 150, 80, 0);
        } catch (Exception e) {}
        cb.endText();

        document.close();
    }

    private byte[] generateQRCode(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}
