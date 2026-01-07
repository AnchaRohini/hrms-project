package com.mentis.hrms.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationService.class);

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    public String generateOfferPdf(String htmlContent, String candidateName, String offerType, Long offerId) throws Exception {
        logger.info("Generating PDF for offer ID: {}", offerId);

        try {
            // Create offers directory if not exists
            Path offersDir = Paths.get(basePath, "offers");
            Files.createDirectories(offersDir);

            // Generate file name
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeName = candidateName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = String.format("%s_%s_%d_%s.pdf",
                    safeName, offerType, offerId, timestamp);

            Path pdfPath = offersDir.resolve(fileName);

            // Convert HTML to PDF using the working method
            try (FileOutputStream fos = new FileOutputStream(pdfPath.toFile())) {
                HtmlConverter.convertToPdf(htmlContent, fos);
                logger.info("PDF generated successfully: {}", pdfPath);
            }

            // Create HTML backup
            Path htmlPath = offersDir.resolve(fileName.replace(".pdf", ".html"));
            Files.write(htmlPath, htmlContent.getBytes());
            logger.info("HTML backup created: {}", htmlPath);

            return "offers/" + fileName;

        } catch (Exception e) {
            logger.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    public String generateSimplePdf(String content, String candidateName, Long offerId) throws Exception {
        logger.info("Generating simple PDF for offer ID: {}", offerId);

        try {
            Path offersDir = Paths.get(basePath, "offers");
            Files.createDirectories(offersDir);

            String fileName = String.format("offer_%d_%s.pdf",
                    offerId,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            Path pdfPath = offersDir.resolve(fileName);

            // Create simple PDF
            try (PdfWriter writer = new PdfWriter(pdfPath.toFile());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                document.add(new Paragraph("Offer Letter")
                        .setFontSize(18)
                        .setBold());
                document.add(new Paragraph(content));
            }

            logger.info("Simple PDF generated: {}", pdfPath);
            return "offers/" + fileName;

        } catch (Exception e) {
            logger.error("Error in simple PDF generation: {}", e.getMessage(), e);
            throw e;
        }
    }
}