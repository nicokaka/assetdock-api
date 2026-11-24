package com.assetdock.api.asset.application;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class AssetLabelService {

    private static final int QR_SIZE = 250;

    public byte[] generateQrCodePng(String assetTag) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(assetTag, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR Code", e);
        }
    }

    public byte[] generateLabelPdf(String assetTag, String displayName, String serialNumber) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(300, 150)); // Etiqueta pequena
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Fonte padrão
                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Desenhar texto
                contentStream.beginText();
                contentStream.setFont(fontBold, 14);
                contentStream.newLineAtOffset(10, 120);
                contentStream.showText("AssetDock");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(fontRegular, 10);
                contentStream.newLineAtOffset(10, 100);
                contentStream.showText("Tag: " + sanitizeWinAnsi(fontRegular, assetTag != null ? assetTag : "N/A"));
                contentStream.newLineAtOffset(0, -15);
                String safeDisplayName = sanitizeWinAnsi(fontRegular, displayName != null ? displayName : "N/A");
                contentStream.showText("Name: " + safeDisplayName);
                contentStream.newLineAtOffset(0, -15);
                String safeSerial = sanitizeWinAnsi(fontRegular, serialNumber != null ? serialNumber : "N/A");
                contentStream.showText("Serial: " + safeSerial);
                contentStream.endText();

                // Gerar e desenhar QR Code na lateral direita
                byte[] qrCodeBytes = generateQrCodePng(assetTag);
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, qrCodeBytes, "QR Code");
                contentStream.drawImage(pdImage, 180, 20, 100, 100);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Label PDF", e);
        }
    }

    private String sanitizeWinAnsi(PDType1Font font, String value) {
        if (value == null) {
            return "N/A";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        StringBuilder sanitized = new StringBuilder(normalized.length());
        int index = 0;
        while (index < normalized.length()) {
            int codePoint = normalized.codePointAt(index);
            index += Character.charCount(codePoint);
            if (Character.isISOControl(codePoint)) {
                sanitized.append(' ');
                continue;
            }
            String glyph = new String(Character.toChars(codePoint));
            try {
                font.encode(glyph);
                sanitized.append(glyph);
            } catch (Exception ignored) {
                sanitized.append('?');
            }
        }
        return sanitized.toString();
    }
}
