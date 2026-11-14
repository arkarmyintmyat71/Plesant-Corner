package com.furiousfive.PleasantCorner.controller;

import com.furiousfive.PleasantCorner.model.MenuItem;
import com.furiousfive.PleasantCorner.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemApiController {

    private final MenuItemService service;
    private final com.furiousfive.PleasantCorner.repository.OrderRepository orderRepository;

    @GetMapping
    public List<MenuItem> getAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<MenuItem> create(@RequestBody MenuItem item) {
        return ResponseEntity.ok(service.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItem> update(@PathVariable Long id, @RequestBody MenuItem item) {
        item.setId(id);
        return ResponseEntity.ok(service.save(item));
    }

    @PatchMapping("/{id}/available")
    public ResponseEntity<?> toggleAvailable(@PathVariable Long id,
                                              @RequestBody Map<String, Boolean> body) {
        MenuItem item = service.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MenuItem not found: " + id));
        item.setAvailable(body.getOrDefault("available", item.isAvailable()));
        return ResponseEntity.ok(service.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        // Past orders reference the item; deleting it would break order history
        if (orderRepository.countOrderItemsForMenuItem(id) > 0) {
            return ResponseEntity.status(409).body(Map.of("error",
                "This item appears in past orders, so it can't be deleted. Mark it unavailable instead."));
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
