package vn.edu.ute.sportute.util;

import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {
    private static final Set<String> PREFIXES = Set.of("KH", "TK", "NV", "S", "M", "BG", "CTG", "BT", "KM", "DS", "BL", "CT", "GD", "DV", "CTDV", "KEO");
    private final JdbcTemplate jdbc;

    public IdGenerator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public String generateId(String prefix) {
        if (!PREFIXES.contains(prefix)) throw new IllegalArgumentException("Tiền tố mã không hợp lệ");
        // SQL Server sequences are atomic across threads/instances and survive restarts.
        // Rolled-back transactions may leave gaps; identifiers are never recycled.
        Long next = jdbc.queryForObject("SELECT NEXT VALUE FOR dbo.SEQ_" + prefix, Long.class);
        if (next == null || next < 1) throw new IllegalStateException("Không thể cấp mã dữ liệu");
        String id = prefix + String.format(java.util.Locale.ROOT, "%03d", next);
        if (id.length() > 10) throw new IllegalStateException("Đã hết dải mã " + prefix);
        return id;
    }
}
