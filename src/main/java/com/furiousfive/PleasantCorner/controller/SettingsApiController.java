package com.furiousfive.PleasantCorner.controller;

import com.furiousfive.PleasantCorner.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Saves the public base URL used in table QR codes.
 * The value is applied immediately in QrCodeService and also written to
 * app-override.properties (imported in application.yaml) so it survives a
 * restart. On hosts with an ephemeral disk (e.g. Render) the file is lost on
 * redeploy — set APP_PUBLIC_BASE_URL there instead.
 */
@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsApiController {

    private final QrCodeService qrCodeService;

    /** POST /api/settings/ngrok  body: { "ngrokBaseUrl": "https://..." } */
    @PostMapping("/ngrok")
    public ResponseEntity<?> saveBaseUrl(@RequestBody Map<String, String> body) {
        String url = body.getOrDefault("ngrokBaseUrl", "").trim();

        if (!url.isEmpty() && !url.matches("(?i)^https?://[^\\s/]+.*$")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "URL must start with http:// or https://"));
        }

        try {
            Files.writeString(Paths.get("app-override.properties"),
                "app.public-base-url=" + url + "\n");
        } catch (IOException e) {
            // Not fatal: the in-memory value below still applies until restart
            log.warn("Could not write app-override.properties: {}", e.getMessage());
        }

        qrCodeService.setBaseUrl(url);

        return ResponseEntity.ok(Map.of(
            "saved", true,
            "ngrokBaseUrl", qrCodeService.getBaseUrl(),
            "note", "Saved. Regenerate QR codes now."
        ));
    }
}
