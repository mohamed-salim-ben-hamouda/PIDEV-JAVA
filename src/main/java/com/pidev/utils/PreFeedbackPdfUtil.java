package com.pidev.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONArray;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PreFeedbackPdfUtil {
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float PAGE_W = PAGE_SIZE.getWidth();
    private static final float PAGE_H = PAGE_SIZE.getHeight();

    private static final float MARGIN = 52f;
    private static final float CONTENT_W = PAGE_W - (MARGIN * 2f);

    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final float TITLE_SIZE = 18f;
    private static final float H2_SIZE = 12.5f;
    private static final float BODY_SIZE = 10.8f;
    private static final float SMALL_SIZE = 9.2f;
    private static final float LEADING = 14f;
    private static final Color BRAND_BLUE = new Color(0x52, 0x66, 0xEB);
    private static final Color TEXT_DARK = new Color(0x16, 0x1a, 0x1d);
    private static final Color MUTED = new Color(0x6b, 0x72, 0x80);
    private static final Color BOX_BG = new Color(0xf5, 0xf7, 0xfa);
    private static final Color BOX_BORDER = new Color(0xe5, 0xe7, 0xeb);

    private PreFeedbackPdfUtil() {
    }

    /**
     * Writes a "Pre-Feedback" PDF from the Flowise JSON (either the inner object or a wrapper containing "json").
     *
     * @param preFeedbackJson  JSON string stored in DB (from FlowiseGraderUtil)
     * @param outputDir        directory where the PDF should be created
     * @param outputFileName   file name, e.g. "prefeedback-activity-12.pdf"
     */
    public static Path writePreFeedbackPdf(String preFeedbackJson, Path outputDir, String outputFileName) throws IOException {
        Objects.requireNonNull(outputDir, "outputDir");
        if (outputFileName == null || outputFileName.isBlank()) {
            throw new IllegalArgumentException("outputFileName is required.");
        }

        JSONObject root = safeParseObject(preFeedbackJson);
        JSONObject payload = unwrapPayload(root);

        Files.createDirectories(outputDir);
        Path out = outputDir.resolve(outputFileName);

        try (PDDocument doc = new PDDocument()) {
            PdfCursor cursor = new PdfCursor(doc);
            try {
                cursor.newPage();

                drawHeader(cursor, payload);
                cursor.y -= 18f;

                if (payload.optBoolean("error", false)) {
                    drawSectionTitle(cursor, "Pre-Feedback Error");
                    cursor.y -= 6f;
                    drawBoxedParagraph(cursor, payload.optString("message", "Unknown error."), BODY_SIZE);
                    cursor.closeStream();
                    save(doc, out);
                    return out;
                }

                drawScoreAndCriteria(cursor, payload);
                cursor.y -= 10f;

                drawListSection(cursor, "Strengths", payload.optJSONArray("strengths"));
                cursor.y -= 4f;
                drawListSection(cursor, "Weaknesses", payload.optJSONArray("weaknesses"));
                cursor.y -= 4f;
                drawListSection(cursor, "Missing Requirements", payload.optJSONArray("missing_requirements"));
                cursor.y -= 6f;

                drawSectionTitle(cursor, "Final Feedback");
                cursor.y -= 6f;
                drawBoxedParagraph(cursor, payload.optString("final_feedback", ""), BODY_SIZE);

                cursor.y -= 14f;
                drawFooter(cursor);

                cursor.closeStream();
                save(doc, out);
                return out;
            } finally {
                cursor.closeStream();
            }
        }
    }

    public static Path writePreFeedbackPdfToResources(String preFeedbackJson, String outputFileName) throws IOException {
        Path outputDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "challenge_module", "prefeedback_pdf");
        return writePreFeedbackPdf(preFeedbackJson, outputDir, outputFileName);
    }

    private static void save(PDDocument doc, Path out) throws IOException {
        Files.deleteIfExists(out);
        doc.save(out.toFile());
    }

    private static void drawHeader(PdfCursor cursor, JSONObject payload) throws IOException {
        float headerH = 74f;

        cursor.cs.setNonStrokingColor(BRAND_BLUE);
        cursor.cs.addRect(0, PAGE_H - headerH, PAGE_W, headerH);
        cursor.cs.fill();

        cursor.cs.setNonStrokingColor(Color.WHITE);
        drawText(cursor, FONT_BOLD, TITLE_SIZE, MARGIN, PAGE_H - 44f, "Pre-Feedback Report");

        double overall = payload.optDouble("overall_score", Double.NaN);
        String scoreText = Double.isNaN(overall) ? "Score: N/A" : ("Score: " + trimNumber(overall) + " / 20");
        float scoreW = textWidth(FONT_BOLD, 12.5f, scoreText);

        drawText(cursor, FONT_BOLD, 12.5f, PAGE_W - MARGIN - scoreW, PAGE_H - 44f, scoreText);

        String generated = "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        float genW = textWidth(FONT, SMALL_SIZE, generated);
        drawText(cursor, FONT, SMALL_SIZE, PAGE_W - MARGIN - genW, PAGE_H - 62f, generated);

        cursor.cs.setNonStrokingColor(TEXT_DARK);
        cursor.y = PAGE_H - headerH - 20f;
    }

    private static void drawFooter(PdfCursor cursor) throws IOException {
        ensureSpace(cursor, 40f);

        cursor.cs.setStrokingColor(BOX_BORDER);
        cursor.cs.setLineWidth(1f);
        cursor.cs.moveTo(MARGIN, cursor.y);
        cursor.cs.lineTo(PAGE_W - MARGIN, cursor.y);
        cursor.cs.stroke();

        cursor.y -= 14f;
        cursor.cs.setNonStrokingColor(MUTED);
        drawText(cursor, FONT, SMALL_SIZE, MARGIN, cursor.y, "This pre-feedback is auto-generated. Please review before final grading.");
        cursor.cs.setNonStrokingColor(TEXT_DARK);
    }

    private static void drawScoreAndCriteria(PdfCursor cursor, JSONObject payload) throws IOException {
        drawSectionTitle(cursor, "Scores");
        cursor.y -= 6f;

        JSONObject criteria = payload.optJSONObject("criteria");
        String completeness = safeScore(criteria, "completeness");
        String accuracy = safeScore(criteria, "accuracy");
        String clarity = safeScore(criteria, "clarity");
        String structure = safeScore(criteria, "structure");

        List<Row> rows = List.of(
                new Row("Completeness", completeness),
                new Row("Accuracy", accuracy),
                new Row("Clarity", clarity),
                new Row("Structure", structure)
        );

        float boxH = 18f + (rows.size() * 18f) + 10f;
        ensureSpace(cursor, boxH + 8f);

        float x = MARGIN;
        float yTop = cursor.y;
        float boxY = yTop - boxH;

        cursor.cs.setNonStrokingColor(BOX_BG);
        cursor.cs.addRect(x, boxY, CONTENT_W, boxH);
        cursor.cs.fill();

        cursor.cs.setStrokingColor(BOX_BORDER);
        cursor.cs.setLineWidth(1f);
        cursor.cs.addRect(x, boxY, CONTENT_W, boxH);
        cursor.cs.stroke();

        float y = yTop - 18f;
        cursor.cs.setNonStrokingColor(TEXT_DARK);
        drawText(cursor, FONT_BOLD, BODY_SIZE, x + 14f, y, "Criteria");
        drawText(cursor, FONT_BOLD, BODY_SIZE, x + CONTENT_W - 76f, y, "Score / 20");

        cursor.cs.setStrokingColor(BOX_BORDER);
        cursor.cs.moveTo(x + 12f, y - 6f);
        cursor.cs.lineTo(x + CONTENT_W - 12f, y - 6f);
        cursor.cs.stroke();

        y -= 20f;
        for (Row row : rows) {
            drawText(cursor, FONT, BODY_SIZE, x + 14f, y, row.label);
            drawTextRightAligned(cursor, FONT_BOLD, BODY_SIZE, x + CONTENT_W - 14f, y, row.value);
            y -= 18f;
        }

        cursor.y = boxY - 10f;
    }

    private static void drawListSection(PdfCursor cursor, String title, JSONArray items) throws IOException {
        drawSectionTitle(cursor, title);
        cursor.y -= 6f;

        if (items == null || items.length() == 0) {
            drawMutedText(cursor, "None.");
            cursor.y -= 6f;
            return;
        }

        for (int i = 0; i < items.length(); i++) {
            String item = String.valueOf(items.opt(i));
            if (item == null || item.isBlank()) {
                continue;
            }
            drawBullet(cursor, item);
            cursor.y -= 2f;
        }
        cursor.y -= 6f;
    }

    private static void drawBoxedParagraph(PdfCursor cursor, String paragraph, float fontSize) throws IOException {
        String safe = paragraph == null ? "" : paragraph.trim();
        if (safe.isEmpty()) {
            drawMutedText(cursor, "No feedback provided.");
            cursor.y -= 6f;
            return;
        }

        List<String> lines = wrapText(safe, FONT, fontSize, CONTENT_W - 28f);
        float boxH = 18f + (lines.size() * LEADING) + 10f;

        ensureSpace(cursor, boxH + 6f);

        float x = MARGIN;
        float yTop = cursor.y;
        float boxY = yTop - boxH;

        cursor.cs.setNonStrokingColor(BOX_BG);
        cursor.cs.addRect(x, boxY, CONTENT_W, boxH);
        cursor.cs.fill();

        cursor.cs.setStrokingColor(BOX_BORDER);
        cursor.cs.setLineWidth(1f);
        cursor.cs.addRect(x, boxY, CONTENT_W, boxH);
        cursor.cs.stroke();

        float y = yTop - 18f;
        cursor.cs.setNonStrokingColor(TEXT_DARK);
        for (String line : lines) {
            drawText(cursor, FONT, fontSize, x + 14f, y, line);
            y -= LEADING;
        }

        cursor.y = boxY - 10f;
    }

    private static void drawSectionTitle(PdfCursor cursor, String title) throws IOException {
        ensureSpace(cursor, 34f);
        cursor.cs.setNonStrokingColor(TEXT_DARK);
        drawText(cursor, FONT_BOLD, H2_SIZE, MARGIN, cursor.y, title);
        cursor.y -= 10f;
        cursor.cs.setStrokingColor(BOX_BORDER);
        cursor.cs.setLineWidth(1f);
        cursor.cs.moveTo(MARGIN, cursor.y);
        cursor.cs.lineTo(PAGE_W - MARGIN, cursor.y);
        cursor.cs.stroke();
        cursor.y -= 10f;
    }

    private static void drawMutedText(PdfCursor cursor, String text) throws IOException {
        ensureSpace(cursor, 18f);
        cursor.cs.setNonStrokingColor(MUTED);
        drawText(cursor, FONT, BODY_SIZE, MARGIN, cursor.y, text);
        cursor.cs.setNonStrokingColor(TEXT_DARK);
    }

    private static void drawBullet(PdfCursor cursor, String text) throws IOException {
        float indent = 14f;
        float bulletGap = 9f;
        float maxW = CONTENT_W - indent - bulletGap;

        List<String> lines = wrapText(text, FONT, BODY_SIZE, maxW);
        for (int i = 0; i < lines.size(); i++) {
            ensureSpace(cursor, LEADING + 6f);
            if (i == 0) {
                cursor.cs.setNonStrokingColor(BRAND_BLUE);
                drawText(cursor, FONT_BOLD, BODY_SIZE + 1f, MARGIN, cursor.y, "•");
                cursor.cs.setNonStrokingColor(TEXT_DARK);
            }
            float x = MARGIN + indent;
            drawText(cursor, FONT, BODY_SIZE, x, cursor.y, lines.get(i));
            cursor.y -= LEADING;
        }
    }
    private static void drawText(PdfCursor cursor, PDType1Font font, float fontSize, float x, float y, String text) throws IOException {
        cursor.cs.beginText();
        cursor.cs.setFont(font, fontSize);
        cursor.cs.newLineAtOffset(x, y);
        cursor.cs.showText(sanitize(text));
        cursor.cs.endText();
    }
    private static void drawTextRightAligned(PdfCursor cursor, PDType1Font font, float fontSize, float rightX, float y, String text) throws IOException {
        float w = textWidth(font, fontSize, text);
        drawText(cursor, font, fontSize, rightX - w, y, text);
    }

    private static float textWidth(PDType1Font font, float fontSize, String text) throws IOException {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        return (font.getStringWidth(sanitize(text)) / 1000f) * fontSize;
    }

    private static void ensureSpace(PdfCursor cursor, float neededHeight) throws IOException {
        if (cursor.y - neededHeight < MARGIN) {
            cursor.newPage();
            cursor.y = PAGE_H - MARGIN;
        }
    }

    private static List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        String safe = sanitize(text).replaceAll("\\s+", " ").trim();
        if (safe.isEmpty()) {
            return List.of("");
        }

        String[] words = safe.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String next = line.length() == 0 ? word : (line + " " + word);
            float w = (font.getStringWidth(next) / 1000f) * fontSize;
            if (w <= maxWidth) {
                line.setLength(0);
                line.append(next);
                continue;
            }
            if (line.length() != 0) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                lines.add(word);
            }
        }
        if (line.length() != 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static JSONObject safeParseObject(String json) {
        if (json == null || json.isBlank()) {
            return new JSONObject().put("error", true).put("message", "Empty pre_feedback JSON.");
        }
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return new JSONObject(json.substring(start, end + 1));
                } catch (Exception ignored) {
                }
            }
            return new JSONObject().put("error", true).put("message", "Invalid JSON in pre_feedback.");
        }
    }

    private static JSONObject unwrapPayload(JSONObject root) {
        if (root == null) {
            return new JSONObject().put("error", true).put("message", "Missing pre_feedback payload.");
        }
        Object inner = root.opt("json");
        if (inner instanceof JSONObject obj) {
            return obj;
        }
        if (inner instanceof String s) {
            return safeParseObject(s);
        }
        return root;
    }

    private static String safeScore(JSONObject criteria, String key) {
        if (criteria == null) {
            return "N/A";
        }
        if (!criteria.has(key)) {
            return "N/A";
        }
        double v = criteria.optDouble(key, Double.NaN);
        return Double.isNaN(v) ? "N/A" : trimNumber(v);
    }

    private static String trimNumber(double v) {
        if (v == (long) v) {
            return String.valueOf((long) v);
        }
        String s = String.valueOf(v);
        return s;
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\r", " ").replace("\t", " ").replace("\u0000", "");
    }

    private record Row(String label, String value) {
    }

    private static final class PdfCursor {
        private final PDDocument doc;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        private PdfCursor(PDDocument doc) {
            this.doc = doc;
        }

        private void newPage() throws IOException {
            closeStream();
            page = new PDPage(PAGE_SIZE);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PAGE_H - MARGIN;
            cs.setNonStrokingColor(TEXT_DARK);
        }

        private void closeStream() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }
    }
}
