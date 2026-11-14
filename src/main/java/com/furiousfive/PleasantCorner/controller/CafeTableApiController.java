package com.furiousfive.PleasantCorner.controller;

import com.furiousfive.PleasantCorner.model.CafeTable;
import com.furiousfive.PleasantCorner.repository.CafeTableRepository;
import com.furiousfive.PleasantCorner.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class CafeTableApiController {

    private final CafeTableRepository repo;
    private final QrCodeService       qrCodeService;
    private final com.furiousfive.PleasantCorner.repository.OrderRepository orderRepository;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream().map(t -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",             t.getId());
            m.put("tableNumber",    t.getTableNumber());
            m.put("seats",          t.getSeats());
            m.put("locationNote",   t.getLocationNote());
            m.put("status",         t.getStatus());
            m.put("occupied",       t.isOccupied());
            m.put("customerUrl",    t.getCustomerUrl());
            m.put("tableQrDataUrl", t.getTableQrDataUrl());
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<CafeTable> create(@RequestBody CafeTable item) {
        item.setId(null);
        // Auto-generate QR code for this table
        String url = qrCodeService.buildCustomerUrl(item.getTableNumber());
        item.setCustomerUrl(url);
        item.setTableQrDataUrl(qrCodeService.generateQrDataUrl(url));
        return ResponseEntity.ok(repo.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CafeTable> update(@PathVariable Long id, @RequestBody CafeTable item) {
        // Update only the fields the edit form sends, so the payment QR and
        // occupied flag of the existing table are kept
        CafeTable existing = repo.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (item.getTableNumber() != null) existing.setTableNumber(item.getTableNumber());
        existing.setSeats(item.getSeats());
        existing.setLocationNote(item.getLocationNote());
        if (item.getStatus() != null) existing.setStatus(item.getStatus());
        // Regenerate QR in case table number or public URL changed
        String url = qrCodeService.buildCustomerUrl(existing.getTableNumber());
        existing.setCustomerUrl(url);
        existing.setTableQrDataUrl(qrCodeService.generateQrDataUrl(url));
        return ResponseEntity.ok(repo.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        // Orders reference their table; deleting it would break order history
        if (orderRepository.countByTable_Id(id) > 0) {
            return ResponseEntity.status(409).body(Map.of("error",
                "This table has order history, so it can't be deleted. Set it to inactive instead."));
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/tables/regenerate-all-qr
     * Called by admin after changing the ngrok URL in settings.
     * Regenerates QR codes for every table at once.
     */
    @PatchMapping("/regenerate-all-qr")
    public ResponseEntity<?> regenerateAllQr() {
        List<CafeTable> tables = repo.findAll();
        for (CafeTable t : tables) {
            String url = qrCodeService.buildCustomerUrl(t.getTableNumber());
            t.setCustomerUrl(url);
            t.setTableQrDataUrl(qrCodeService.generateQrDataUrl(url));
        }
        repo.saveAll(tables);
        return ResponseEntity.ok(Map.of("regenerated", tables.size()));
    }

    /**
     * PATCH /api/tables/{id}/payment-qr
     * Saves the payment QR image (base64 data URL) for a table.
     * Called from the Payments page when admin uploads their PromptPay QR.
     * The same payment QR is broadcast to all tables.
     */
    @PatchMapping("/payment-qr")
    public ResponseEntity<?> savePaymentQrForAll(@RequestBody Map<String, String> body) {
        String dataUrl = body.get("paymentQrDataUrl");
        if (dataUrl == null || dataUrl.isBlank())
            return ResponseEntity.badRequest().body("Missing paymentQrDataUrl");
        List<CafeTable> tables = repo.findAll();
        tables.forEach(t -> t.setPaymentQrDataUrl(dataUrl));
        repo.saveAll(tables);
        return ResponseEntity.ok(Map.of("saved", tables.size()));
    }
}
