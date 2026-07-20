package ee.matteus.pannukas.gui;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class PdfReportExporter {
    private PdfReportExporter() {
    }

    static void export(File file, String planName, BufferedImage mapImage, String reportText) throws IOException {
        try (PDDocument document = new PDDocument()) {
            addMapPage(document, planName, mapImage);
            addReportPages(document, reportText);
            addPageNumbers(document);
            document.save(file);
        }
    }

    private static void addMapPage(PDDocument document, String planName, BufferedImage mapImage) throws IOException {
        PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        PDPage page = new PDPage(pageSize);
        document.addPage(page);

        float margin = 36;
        float titleSize = 16;
        float availableWidth = pageSize.getWidth() - margin * 2;
        float availableHeight = pageSize.getHeight() - margin * 2 - 28;
        float scale = Math.min(availableWidth / mapImage.getWidth(), availableHeight / mapImage.getHeight());
        float imageWidth = mapImage.getWidth() * scale;
        float imageHeight = mapImage.getHeight() * scale;
        float imageX = margin + (availableWidth - imageWidth) / 2;
        float imageY = margin;

        PDImageXObject pdfImage = LosslessFactory.createFromImage(document, mapImage);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, titleSize);
            content.newLineAtOffset(margin, pageSize.getHeight() - margin - titleSize);
            content.showText(planName);
            content.endText();
            content.drawImage(pdfImage, imageX, imageY, imageWidth, imageHeight);
        }
    }

    private static void addReportPages(PDDocument document, String reportText) throws IOException {
        float margin = 50;
        float fontSize = 10;
        float leading = 14;
        PDType1Font regularFont = PDType1Font.HELVETICA;
        PDType1Font boldFont = PDType1Font.HELVETICA_BOLD;
        PDRectangle pageSize = PDRectangle.A4;
        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        PDPageContentStream content = new PDPageContentStream(document, page);
        float y = pageSize.getHeight() - margin;
        try {
            for (String originalLine : reportText.split("\\R", -1)) {
                String trimmedLine = originalLine.trim();
                if (!trimmedLine.isBlank() && trimmedLine.chars().allMatch(character -> character == '=')) {
                    continue;
                }
                PdfReportLineStyle style = pdfReportLineStyle(originalLine, regularFont, boldFont, fontSize);
                y -= style.extraSpaceBefore();
                float maxLineWidth = pageSize.getWidth() - margin * 2 - style.indent();
                for (String line : wrapLine(originalLine.trim(), style.font(), style.fontSize(), maxLineWidth)) {
                    if (y <= margin + 20) {
                        content.close();
                        page = new PDPage(pageSize);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = pageSize.getHeight() - margin;
                    }
                    content.beginText();
                    content.setFont(style.font(), style.fontSize());
                    content.newLineAtOffset(margin + style.indent(), y);
                    content.showText(line);
                    content.endText();
                    y -= style.lineHeight(leading);
                }
            }
        } finally {
            content.close();
        }
    }

    private static PdfReportLineStyle pdfReportLineStyle(
            String line,
            PDType1Font regularFont,
            PDType1Font boldFont,
            float defaultFontSize
    ) {
        String trimmedLine = line.trim();
        if (trimmedLine.isBlank()) {
            return new PdfReportLineStyle(regularFont, defaultFontSize, 0, 4);
        }
        if (isHeadingLine(line)) {
            return new PdfReportLineStyle(boldFont, defaultFontSize + 1, 0, 10);
        }
        int leadingSpaces = line.length() - line.stripLeading().length();
        return new PdfReportLineStyle(regularFont, defaultFontSize, leadingSpaces * 5.0f, 0);
    }

    private static boolean isHeadingLine(String line) {
        String trimmedLine = line.trim();
        return !trimmedLine.isBlank() && !line.startsWith(" ");
    }

    private static void addPageNumbers(PDDocument document) throws IOException {
        int pageCount = document.getNumberOfPages();
        for (int index = 0; index < pageCount; index++) {
            PDPage page = document.getPage(index);
            PDRectangle pageSize = page.getMediaBox();
            String pageNumber = "lk %d / %d".formatted(index + 1, pageCount);
            float fontSize = 9;
            float textWidth = textWidth(pageNumber, PDType1Font.HELVETICA, fontSize);
            try (PDPageContentStream content = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            )) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, fontSize);
                content.newLineAtOffset((pageSize.getWidth() - textWidth) / 2, 24);
                content.showText(pageNumber);
                content.endText();
            }
        }
    }

    private static List<String> wrapLine(String line, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        if (line.isBlank()) {
            return List.of("");
        }

        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : line.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (textWidth(candidate, font, fontSize) <= maxWidth || currentLine.isEmpty()) {
                currentLine.setLength(0);
                currentLine.append(candidate);
            } else {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }
        lines.add(currentLine.toString());
        return lines;
    }

    private static float textWidth(String text, PDType1Font font, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }

    private record PdfReportLineStyle(PDType1Font font, float fontSize, float indent, float extraSpaceBefore) {
        private float lineHeight(float defaultLineHeight) {
            if (fontSize > 10) {
                return defaultLineHeight + 2;
            }
            return defaultLineHeight;
        }
    }
}
