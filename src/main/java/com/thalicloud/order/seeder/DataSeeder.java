package com.thalicloud.order.seeder;

import com.thalicloud.order.entity.Vendor;
import com.thalicloud.order.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Inserts realistic test data on startup when the current vendor has no orders.
 * Runs only on non-test profiles. Auth-service's ddl-auto:create wipes vendor rows
 * on every restart, so this re-seeds automatically after each fresh registration.
 *
 * Seed summary (relative to today):
 *   - 10 vendor_orders  (4 yesterday · 6 today — mix of all four statuses)
 *   - 1  vendor_meal_plan (active "June Plan", ends 2026-06-30)
 *   - 5  vendor_ratings  (avg ≈ 4.7 / 5 reviews)
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final VendorRepository vendorRepository;
    private final JdbcTemplate     jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var vendors = vendorRepository.findAll();
        if (vendors.isEmpty()) {
            log.info("[DataSeeder] No vendors found — register first, then restart to seed data.");
            return;
        }

        var vendor   = vendors.get(0);
        var vendorId = vendor.getId();

        Long existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vendor_orders WHERE vendor_id = ?",
                Long.class, vendorId);

        if (existing != null && existing > 0) {
            log.info("[DataSeeder] Seed data already present for {} — skipping.", vendor.getEmail());
            return;
        }

        // Remove orphaned seed orders left by a previous vendor registration (same display IDs, old vendor_id)
        jdbcTemplate.update(
                "DELETE FROM vendor_orders WHERE order_display_id IN " +
                "('ORD-001','ORD-002','ORD-003','ORD-004','ORD-005'," +
                " 'ORD-006','ORD-007','ORD-008','ORD-009','ORD-010')");

        log.info("[DataSeeder] Seeding test data for vendor: {} ({})", vendor.getEmail(), vendorId);
        seedOrders(vendorId);
        seedMealPlan(vendorId);
        seedRatings(vendorId, RATING_PATTERNS[0]);
        log.info("[DataSeeder] Done — 10 orders, 1 meal plan, 5 ratings inserted.");

        seedRatingsForUnratedVendors(vendors);
    }

    // ── Kitchen-discovery ratings (SRS M3) ───────────────────────────────────────
    // vendor-service's /api/kitchens listing calls GET /api/orders/ratings/aggregate
    // to enrich each kitchen card. Any vendor without ratings yet (e.g. the demo
    // kitchens seeded by auth-service's DataSeeder) gets a small varied rating set
    // here so the Home screen doesn't show every kitchen as unrated.
    private static final double[][] RATING_PATTERNS = {
            {4.5, 4.8, 4.7, 5.0, 4.6},
            {4.2, 4.0, 4.5, 4.1, 3.9},
            {3.8, 4.0, 3.5, 4.2},
            {4.9, 5.0, 4.8, 4.9, 5.0, 4.7},
            {4.3, 4.6, 4.4},
    };

    private void seedRatingsForUnratedVendors(List<Vendor> vendors) {
        for (int i = 0; i < vendors.size(); i++) {
            UUID vendorId = vendors.get(i).getId();
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vendor_ratings WHERE vendor_id = ?", Long.class, vendorId);
            if (count != null && count > 0) continue;

            double[] pattern = RATING_PATTERNS[i % RATING_PATTERNS.length];
            seedRatings(vendorId, pattern);
        }
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    private void seedOrders(UUID vendorId) {
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Columns: id, order_display_id, vendor_id, customer_name, meal_type,
        //          amount_in_paise, status, created_at, updated_at
        Object[][] rows = {
            // ── Yesterday (all Delivered) ─────────────────────────────────────
            row("ORD-001", vendorId, "Priya Desai",     "Standard Veg",     29000L, "DELIVERED", yesterday.atTime(8, 0)),
            row("ORD-002", vendorId, "Amit Kulkarni",   "Mini Veg",         13000L, "DELIVERED", yesterday.atTime(8, 30)),
            row("ORD-003", vendorId, "Sneha Joshi",     "Custom",           17500L, "DELIVERED", yesterday.atTime(9, 0)),
            row("ORD-004", vendorId, "Rajesh Patil",    "Standard Veg",     29000L, "DELIVERED", yesterday.atTime(9, 45)),

            // ── Today ─────────────────────────────────────────────────────────
            row("ORD-005", vendorId, "Meera Shah",      "Premium Non-Veg",  35000L, "DELIVERED", today.atTime(7, 0)),
            row("ORD-006", vendorId, "Vikram Nair",     "Mini Veg",         13000L, "READY",     today.atTime(7, 30)),
            row("ORD-007", vendorId, "Pooja Iyer",      "Standard Veg",     29000L, "PREPARING", today.atTime(8, 0)),
            row("ORD-008", vendorId, "Suresh Kumar",    "Custom",           22000L, "PENDING",   today.atTime(8, 30)),
            row("ORD-009", vendorId, "Ananya Singh",    "Premium Non-Veg",  35000L, "PENDING",   today.atTime(8, 45)),
            row("ORD-010", vendorId, "Kavya Reddy",     "Standard Veg",     29000L, "PENDING",   today.atTime(9, 0)),
        };

        String sql = """
                INSERT INTO vendor_orders
                  (id, order_display_id, vendor_id, customer_name, meal_type,
                   amount_in_paise, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        for (Object[] r : rows) {
            jdbcTemplate.update(sql, r);
        }
    }

    private static Object[] row(
            String displayId, UUID vendorId, String customer,
            String mealType, long paise, String status, LocalDateTime at) {
        return new Object[]{
            UUID.randomUUID(), displayId, vendorId,
            customer, mealType, paise, status, at, at
        };
    }

    // ── Meal plan ─────────────────────────────────────────────────────────────

    private void seedMealPlan(UUID vendorId) {
        LocalDate today     = LocalDate.now();
        // Keep the plan window relative to today so daysRemaining is always meaningful.
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate   = today.withDayOfMonth(today.lengthOfMonth());

        jdbcTemplate.update("""
                INSERT INTO vendor_meal_plans
                  (id, vendor_id, name, start_date, end_date, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, true, ?, ?)
                """,
                UUID.randomUUID(), vendorId,
                today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " Plan",
                startDate, endDate,
                today.atStartOfDay(), today.atStartOfDay()
        );
    }

    // ── Ratings ───────────────────────────────────────────────────────────────

    private void seedRatings(UUID vendorId, double[] values) {
        LocalDateTime now = LocalDateTime.now();

        String sql = """
                INSERT INTO vendor_ratings (id, vendor_id, value, created_at)
                VALUES (?, ?, ?, ?)
                """;

        for (double v : values) {
            jdbcTemplate.update(sql, UUID.randomUUID(), vendorId,
                    new java.math.BigDecimal(String.valueOf(v)), now);
        }
    }
}
