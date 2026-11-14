package com.furiousfive.PleasantCorner.model;

import com.furiousfive.PleasantCorner.enums.Category;
import com.furiousfive.PleasantCorner.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── BASIC INFO ─────────────────────────────
    @Column(nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private String emojiIcon;

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean featuredItem = false;

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    /**
     * Whether this drink item supports temperature selection (Hot/Iced/etc.)
     * Configured by admin in Menu Items. Only relevant when itemType = DRINK.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean supportsDrinkStates = true;

    /**
     * Whether this food/dessert item supports topping add-ons.
     * Configured by admin in Menu Items. Only relevant when itemType = FOOD.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean supportsToppings = true;
}
