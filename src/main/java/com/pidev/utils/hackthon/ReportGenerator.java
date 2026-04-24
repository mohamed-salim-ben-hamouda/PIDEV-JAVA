package com.pidev.utils.hackthon;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.pidev.models.Sponsor;
import com.pidev.models.SponsorHackathon;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportGenerator {

    public static void exportSponsorsToExcel(List<Sponsor> sponsors, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sponsors");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Name", "Website", "Created At"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            for (Sponsor s : sponsors) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getId());
                row.createCell(1).setCellValue(s.getName());
                row.createCell(2).setCellValue(s.getWebsiteUrl());
                row.createCell(3).setCellValue(s.getCreatedAt().format(fmt));
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    public static void generateSponsorshipContract(SponsorHackathon sh, String filePath, String signaturePath) throws IOException {
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Title
            Paragraph title = new Paragraph("SPONSORSHIP AGREEMENT")
                    .setBold().setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(title);

            // Intro
            document.add(new Paragraph("This Agreement is made between:")
                    .setMarginBottom(10));
            
            document.add(new Paragraph("The Organizer of: " + sh.getHackathon().getTitle())
                    .setBold());
            document.add(new Paragraph("AND")
                    .setMarginTop(5).setMarginBottom(5));
            document.add(new Paragraph("The Sponsor: " + sh.getSponsor().getName())
                    .setBold().setMarginBottom(20));

            // Contribution Table
            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            table.addCell(new Paragraph("Contribution Type").setBold());
            table.addCell(sh.getContributionType());
            
            table.addCell(new Paragraph("Contribution Value").setBold());
            table.addCell("$" + String.format("%.2f", sh.getContributionValue()));
            
            document.add(table);

            // Terms
            document.add(new Paragraph("Terms and Conditions:").setBold().setUnderline());
            document.add(new Paragraph("1. The Sponsor agrees to provide the specified contribution before the hackathon start date."));
            document.add(new Paragraph("2. The Organizer agrees to provide logo placement and branding as agreed upon."));
            document.add(new Paragraph("3. This document serves as a binding agreement between both parties."));

            // Signatures
            document.add(new Paragraph("\n\n"));
            Table signTable = new Table(2).useAllAvailableWidth();
            
            // Cell for Organizer
            signTable.addCell(new Paragraph("__________________________\nFor the Organizer").setTextAlignment(TextAlignment.CENTER));
            
            // Cell for Sponsor with Signature Image
            if (signaturePath != null) {
                try {
                    com.itextpdf.layout.element.Image signImg = new com.itextpdf.layout.element.Image(ImageDataFactory.create(signaturePath));
                    signImg.setMaxWidth(100);
                    signImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                    
                    Table sponsorCellTable = new Table(1).useAllAvailableWidth();
                    sponsorCellTable.addCell(new Paragraph("").setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
                    sponsorCellTable.addCell(signImg).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
                    sponsorCellTable.addCell(new Paragraph("__________________________\nFor the Sponsor").setTextAlignment(TextAlignment.CENTER)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
                    
                    signTable.addCell(sponsorCellTable);
                } catch (Exception e) {
                    // Fallback to empty if image fails
                    signTable.addCell(new Paragraph("__________________________\nFor the Sponsor").setTextAlignment(TextAlignment.CENTER));
                }
            } else {
                signTable.addCell(new Paragraph("__________________________\nFor the Sponsor").setTextAlignment(TextAlignment.CENTER));
            }
            
            document.add(signTable);
        }
    }
}
