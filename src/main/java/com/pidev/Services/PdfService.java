package com.pidev.Services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.pidev.models.*;

import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class PdfService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Professional colors
    private static final BaseColor PRIMARY_COLOR = new BaseColor(30, 41, 59); // Dark blue/slate
    private static final BaseColor SECONDARY_COLOR = new BaseColor(71, 85, 105); // Slate gray
    private static final BaseColor ACCENT_COLOR = new BaseColor(37, 99, 235); // Blue

    public void generateCvPdf(Cv cv, String filePath) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, PRIMARY_COLOR);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 16, SECONDARY_COLOR);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, ACCENT_COLOR);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);
        Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, SECONDARY_COLOR);

        // Header Section
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        String name = (cv.getUser() != null ? cv.getUser().getDisplayName() : cv.getNomCv()).toUpperCase();
        PdfPCell nameCell = new PdfPCell(new Phrase(name, titleFont));
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(nameCell);

        String title = (cv.getLangue() != null ? cv.getLangue() : "Candidat").toUpperCase();
        PdfPCell titleCell = new PdfPCell(new Phrase(title, subTitleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(new Paragraph("\n"));

        // Contact Info
        if (cv.getUser() != null) {
            Paragraph contact = new Paragraph();
            contact.add(new Chunk("Email: ", boldFont));
            contact.add(new Chunk(cv.getUser().getEmail(), normalFont));
            if (cv.getLinkedinUrl() != null && !cv.getLinkedinUrl().isBlank()) {
                contact.add(new Chunk("  |  LinkedIn: ", boldFont));
                contact.add(new Chunk(cv.getLinkedinUrl(), normalFont));
            }
            document.add(contact);
        }

        document.add(new Paragraph("\n"));
        document.add(new LineSeparator(1, 100, ACCENT_COLOR, Element.ALIGN_CENTER, -2));
        document.add(new Paragraph("\n"));

        // Summary
        if (cv.getSummary() != null && !cv.getSummary().isBlank()) {
            document.add(new Paragraph("PROFIL", headerFont));
            document.add(new Paragraph(cv.getSummary(), normalFont));
            document.add(new Paragraph("\n"));
        }

        // Experiences
        if (cv.getExperiences() != null && !cv.getExperiences().isEmpty()) {
            document.add(new Paragraph("EXPÉRIENCE PROFESSIONNELLE", headerFont));
            for (Experience exp : cv.getExperiences()) {
                Paragraph expTitle = new Paragraph();
                expTitle.add(new Chunk(exp.getJobTitle(), boldFont));
                expTitle.add(new Chunk(" @ " + exp.getCompany(), normalFont));
                document.add(expTitle);

                String dates = (exp.getStartDate() != null ? exp.getStartDate().format(DATE_FORMATTER) : "")
                        + " - " + (exp.getCurrentlyWorking() != null && exp.getCurrentlyWorking() ? "Présent" : (exp.getEndDate() != null ? exp.getEndDate().format(DATE_FORMATTER) : "Présent"));
                document.add(new Paragraph(dates + " | " + exp.getLocation(), italicFont));

                if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                    Paragraph desc = new Paragraph(exp.getDescription(), normalFont);
                    desc.setSpacingBefore(5);
                    desc.setIndentationLeft(20);
                    document.add(desc);
                }
                document.add(new Paragraph("\n"));
            }
        }

        // Education
        if (cv.getEducations() != null && !cv.getEducations().isEmpty()) {
            document.add(new Paragraph("ÉDUCATION", headerFont));
            for (Education edu : cv.getEducations()) {
                Paragraph eduTitle = new Paragraph();
                eduTitle.add(new Chunk(edu.getDegree() + " en " + edu.getFieldOfStudy(), boldFont));
                document.add(eduTitle);

                String dates = (edu.getStartDate() != null ? edu.getStartDate().format(DATE_FORMATTER) : "")
                        + " - " + (edu.getEndDate() != null ? edu.getEndDate().format(DATE_FORMATTER) : "Présent");
                document.add(new Paragraph(edu.getSchool() + " | " + edu.getCity() + " (" + dates + ")", italicFont));
                document.add(new Paragraph("\n"));
            }
        }

        // Skills & Languages
        PdfPTable skillsLangTable = new PdfPTable(2);
        skillsLangTable.setWidthPercentage(100);
        skillsLangTable.setSpacingBefore(10);

        // Skills Cell
        PdfPCell skillsCell = new PdfPCell();
        skillsCell.setBorder(Rectangle.NO_BORDER);
        skillsCell.addElement(new Paragraph("COMPÉTENCES", headerFont));
        if (cv.getSkills() != null) {
            StringBuilder skillsList = new StringBuilder();
            for (int i = 0; i < cv.getSkills().size(); i++) {
                skillsList.append(cv.getSkills().get(i).getNom());
                if (i < cv.getSkills().size() - 1) skillsList.append(", ");
            }
            skillsCell.addElement(new Paragraph(skillsList.toString(), normalFont));
        }
        skillsLangTable.addCell(skillsCell);

        // Languages Cell
        PdfPCell langCell = new PdfPCell();
        langCell.setBorder(Rectangle.NO_BORDER);
        langCell.addElement(new Paragraph("LANGUES", headerFont));
        if (cv.getLanguages() != null) {
            for (Langue lang : cv.getLanguages()) {
                langCell.addElement(new Paragraph(lang.getNom() + " (" + lang.getNiveau() + ")", normalFont));
            }
        }
        skillsLangTable.addCell(langCell);

        document.add(skillsLangTable);

        // Certifications
        if (cv.getCertifs() != null && !cv.getCertifs().isEmpty()) {
            document.add(new Paragraph("\nCERTIFICATIONS", headerFont));
            for (Certif cert : cv.getCertifs()) {
                Paragraph certPara = new Paragraph();
                certPara.add(new Chunk(cert.getName(), boldFont));
                certPara.add(new Chunk(" par " + cert.getIssuedBy(), normalFont));
                document.add(certPara);
            }
        }

        document.close();
    }
}
