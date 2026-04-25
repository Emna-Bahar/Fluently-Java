package com.example.pijava_fluently.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.imageio.ImageIO;

public class CertificatService {

    private static final String CERTS_DIR = "certificats/";

    private static final BaseColor VIOLET       = new BaseColor(108, 99, 255);
    private static final BaseColor VIOLET_LIGHT = new BaseColor(238, 240, 255);
    private static final BaseColor GRIS_TEXTE   = new BaseColor(60, 60, 60);
    private static final BaseColor OR           = new BaseColor(212, 175, 55);

    public CertificatService() {
        try {
            Files.createDirectories(Paths.get(CERTS_DIR));
        } catch (IOException e) {
            System.err.println("Impossible de créer le dossier certificats : " + e.getMessage());
        }
    }

    /**
     * Génère un certificat PDF sans aucune interaction avec la BD.
     * Le QR code encode les informations directement dans son contenu.
     */
    public String genererCertificat(String prenomNom,
                                    String niveau,
                                    String langue,
                                    double score,
                                    int userId) throws Exception {

        String certUUID = UUID.randomUUID().toString();
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Le QR code encode les infos du certificat en texte simple
        // N'importe qui peut scanner et lire les infos sans serveur
        String contenuQR = "CERTIFICAT FLUENTLY\n"
                + "Titulaire : " + prenomNom + "\n"
                + "Niveau    : " + niveau + " — " + langue + "\n"
                + "Score     : " + String.format("%.0f", score) + "%\n"
                + "Date      : " + date + "\n"
                + "ID        : " + certUUID;

        String nomFichier = CERTS_DIR + "cert_" + userId + "_"
                + niveau + "_" + certUUID.substring(0, 8) + ".pdf";

        byte[] qrBytes = genererQRCode(contenuQR, 150, 150);

        Document doc = new Document(PageSize.A4.rotate(), 40, 40, 40, 40);
        String cheminAbsolu = Paths.get(nomFichier).toAbsolutePath().toString();
        PdfWriter writer = PdfWriter.getInstance(doc,
                new FileOutputStream(cheminAbsolu));
        doc.open();

        float pageW = doc.getPageSize().getWidth();
        float pageH = doc.getPageSize().getHeight();

        // --- FOND ---
        PdfContentByte canvas = writer.getDirectContentUnder();
        canvas.setColorFill(VIOLET_LIGHT);
        canvas.rectangle(0, 0, pageW, pageH);
        canvas.fill();

        // --- BORDURE DOUBLE ---
        canvas.setColorStroke(VIOLET);
        canvas.setLineWidth(3f);
        canvas.rectangle(20, 20, pageW - 40, pageH - 40);
        canvas.stroke();

        canvas.setColorStroke(OR);
        canvas.setLineWidth(1f);
        canvas.rectangle(26, 26, pageW - 52, pageH - 52);
        canvas.stroke();

        // --- BANDE VIOLET EN HAUT ---
        canvas.setColorFill(VIOLET);
        canvas.rectangle(20, pageH - 100, pageW - 40, 80);
        canvas.fill();

        // --- POLICES ---
        BaseFont bfBold    = BaseFont.createFont(BaseFont.HELVETICA_BOLD,
                BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfNormal  = BaseFont.createFont(BaseFont.HELVETICA,
                BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfItalic  = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE,
                BaseFont.CP1252, BaseFont.EMBEDDED);

        PdfContentByte cb = writer.getDirectContent();

        // --- TITRE ---
        cb.beginText();
        cb.setFontAndSize(bfBold, 26);
        cb.setColorFill(BaseColor.WHITE);
        String titre = "CERTIFICAT DE LANGUE — FLUENTLY";
        float titreLargeur = bfBold.getWidthPoint(titre, 26);
        cb.setTextMatrix((pageW - titreLargeur) / 2f, pageH - 68);
        cb.showText(titre);
        cb.endText();

        cb.beginText();
        cb.setFontAndSize(bfNormal, 12);
        cb.setColorFill(BaseColor.WHITE);
        String sousTitre = "Cadre Européen Commun de Référence pour les Langues (CECRL)";
        float stLargeur = bfNormal.getWidthPoint(sousTitre, 12);
        cb.setTextMatrix((pageW - stLargeur) / 2f, pageH - 88);
        cb.showText(sousTitre);
        cb.endText();

        // --- GRAND NIVEAU à gauche ---
        cb.beginText();
        cb.setFontAndSize(bfBold, 80);
        cb.setColorFill(VIOLET);
        cb.setTextMatrix(60, pageH / 2f - 30);
        cb.showText(niveau);
        cb.endText();

        // Trait doré sous le niveau
        cb.setColorStroke(OR);
        cb.setLineWidth(2f);
        cb.moveTo(60, pageH / 2f - 40);
        cb.lineTo(220, pageH / 2f - 40);
        cb.stroke();

        cb.beginText();
        cb.setFontAndSize(bfNormal, 14);
        cb.setColorFill(GRIS_TEXTE);
        cb.setTextMatrix(60, pageH / 2f - 62);
        cb.showText(langue);
        cb.endText();

        // --- TEXTE CENTRAL ---
        float centreX = pageW / 2f;

        cb.beginText();
        cb.setFontAndSize(bfNormal, 14);
        cb.setColorFill(GRIS_TEXTE);
        String txt1 = "Nous certifions que";
        cb.setTextMatrix(centreX - bfNormal.getWidthPoint(txt1, 14) / 2f, pageH - 160);
        cb.showText(txt1);
        cb.endText();

        // NOM
        cb.beginText();
        cb.setFontAndSize(bfBold, 28);
        cb.setColorFill(VIOLET);
        float nomLargeur = bfBold.getWidthPoint(prenomNom, 28);
        cb.setTextMatrix((pageW - nomLargeur) / 2f, pageH - 200);
        cb.showText(prenomNom);
        cb.endText();

        // Ligne sous le nom
        cb.setColorStroke(VIOLET);
        cb.setLineWidth(1f);
        cb.moveTo((pageW - nomLargeur) / 2f - 10, pageH - 207);
        cb.lineTo((pageW + nomLargeur) / 2f + 10, pageH - 207);
        cb.stroke();

        // Description
        cb.beginText();
        cb.setFontAndSize(bfNormal, 13);
        cb.setColorFill(GRIS_TEXTE);
        String desc = "a atteint le niveau " + niveau + " en " + langue
                + " avec un score de " + String.format("%.0f", score) + "%";
        cb.setTextMatrix(centreX - bfNormal.getWidthPoint(desc, 13) / 2f, pageH - 235);
        cb.showText(desc);
        cb.endText();

        // Date
        String dateStr = "Délivré le " + LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy",
                        java.util.Locale.FRENCH));
        cb.beginText();
        cb.setFontAndSize(bfItalic, 12);
        cb.setColorFill(GRIS_TEXTE);
        cb.setTextMatrix(centreX - bfItalic.getWidthPoint(dateStr, 12) / 2f, pageH - 260);
        cb.showText(dateStr);
        cb.endText();

        // --- QR CODE ---
        Image qrImage = Image.getInstance(qrBytes);
        qrImage.setAbsolutePosition(pageW - 180, 50);
        qrImage.scaleAbsolute(130, 130);
        doc.add(qrImage);

        cb.beginText();
        cb.setFontAndSize(bfNormal, 9);
        cb.setColorFill(GRIS_TEXTE);
        cb.setTextMatrix(pageW - 178, 40);
        cb.showText("Scanner pour lire les infos");
        cb.endText();

        // ID unique en petit (sans serveur, juste pour unicité)
        cb.beginText();
        cb.setFontAndSize(bfNormal, 7);
        cb.setColorFill(new BaseColor(150, 150, 150));
        cb.setTextMatrix(pageW - 178, 30);
        cb.showText("ID : " + certUUID.substring(0, 18) + "...");
        cb.endText();

        // --- SIGNATURE ---
        cb.setColorStroke(VIOLET);
        cb.setLineWidth(1.5f);
        cb.moveTo(50, 100);
        cb.lineTo(200, 100);
        cb.stroke();

        cb.beginText();
        cb.setFontAndSize(bfBold, 11);
        cb.setColorFill(VIOLET);
        cb.setTextMatrix(50, 85);
        cb.showText("Fluently Language Platform");
        cb.endText();

        cb.beginText();
        cb.setFontAndSize(bfItalic, 10);
        cb.setColorFill(GRIS_TEXTE);
        cb.setTextMatrix(50, 70);
        cb.showText("Certifié par l'équipe pédagogique Fluently");
        cb.endText();

        doc.close();
        return cheminAbsolu;
    }

    private byte[] genererQRCode(String contenu, int largeur, int hauteur)
            throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE,
                largeur, hauteur);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}