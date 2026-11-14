package com.furiousfive.PleasantCorner.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeService {

    /** Public base URL, e.g. https://pleasant-corner.onrender.com — "" if not configured. */
    private volatile String publicBaseUrl;

    /** Used only when no public URL is configured (local development). */
    private final String localFallback;

    public QrCodeService(@Value("${app.public-base-url:}") String configured,
                         @Value("${server.port:8080}") String port) {
        this.publicBaseUrl = normalize(configured);
        this.localFallback = "http://localhost:" + port;
    }

    /** What the admin configured ("" if nothing) — shown in the Settings input. */
    public String getConfiguredBaseUrl() {
        return publicBaseUrl;
    }

    /** Base URL actually used in QR codes. */
    public String getBaseUrl() {
        return publicBaseUrl.isEmpty() ? localFallback : publicBaseUrl;
    }

    /** Called from the Settings page — takes effect immediately, no restart needed. */
    public void setBaseUrl(String url) {
        this.publicBaseUrl = normalize(url);
    }

    /** Customer URL for a table, e.g. https://.../customer/table/7 */
    public String buildCustomerUrl(int tableNumber) {
        return getBaseUrl() + "/customer/table/" + tableNumber;
    }

    /**
     * Generates a QR code PNG for the given URL and returns it as a
     * base64 data URL: "data:image/png;base64,<data>"
     */
    public String generateQrDataUrl(String url) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            String base64 = Base64.getEncoder().encodeToString(out.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            throw new RuntimeException("QR code generation failed: " + e.getMessage(), e);
        }
    }

    private static String normalize(String url) {
        if (url == null) return "";
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }
}
