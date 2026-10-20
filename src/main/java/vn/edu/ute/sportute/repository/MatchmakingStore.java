package vn.edu.ute.sportute.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.model.MatchmakingPost;
import java.util.List;

/** Match metadata lives in SQL Server; booking/customer/court fields always come from their source rows. */
@Repository
@RequiredArgsConstructor
public class MatchmakingStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private static final String SELECT = """
        SELECT k.Payload, k.MaKeo, k.MaPhieu, kh.MaTK, kh.HoTen, kh.SDT, kh.Email,
               s.MaSan, s.TenSan, m.TenMon, d.GioBD, d.GioKT, b.TrangThai
        FROM dbo.TIMDOITHU k %s
        JOIN dbo.PHIEUDATSAN b ON b.MaPhieu=k.MaPhieu
        JOIN dbo.KHACHHANG kh ON kh.MaKH=b.MaKH
        CROSS APPLY (SELECT TOP (1) * FROM dbo.CHITIETDATSAN cd WHERE cd.MaPhieu=b.MaPhieu ORDER BY cd.GioBD, cd.MaCT) d
        JOIN dbo.SAN s ON s.MaSan=d.MaSan JOIN dbo.MONTHETHAO m ON m.MaMon=s.MaMon
        """;

    private List<MatchmakingPost> query(String hint, String suffix, Object... args) {
        return jdbc.query(SELECT.formatted(hint) + suffix, (rs, row) -> {
            try {
                MatchmakingPost p=json.readValue(rs.getString("Payload"),MatchmakingPost.class);
                p.setId(rs.getString("MaKeo")); p.setBookingId(rs.getString("MaPhieu"));
                p.setCreatorAccountId(rs.getString("MaTK")); p.setCreatorName(rs.getString("HoTen"));
                p.setCreatorPhone(rs.getString("SDT")); p.setCreatorEmail(rs.getString("Email"));
                p.setCourtId(rs.getString("MaSan")); p.setCourtName(rs.getString("TenSan")); p.setSportName(rs.getString("TenMon"));
                var start=rs.getTimestamp("GioBD").toLocalDateTime(); var end=rs.getTimestamp("GioKT").toLocalDateTime();
                p.setMatchDate(start.toLocalDate().toString());
                p.setStartTime(start.toLocalTime().toString()); p.setEndTime(end.toLocalTime().toString());
                p.setDurationMinutes((int)java.time.Duration.between(start,end).toMinutes());
                if ("Đã hủy".equals(rs.getString("TrangThai"))) p.setStatus("CANCELLED");
                return p;
            } catch (java.io.IOException e) { throw new IllegalStateException("Dữ liệu kèo không hợp lệ",e); }
        },args);
    }
    public List<MatchmakingPost> findAll() { return query("",""); }
    public MatchmakingPost findById(String id) {
        return query("WITH (UPDLOCK, HOLDLOCK)"," WHERE k.MaKeo=?",id).stream().findFirst().orElse(null);
    }
    public void save(MatchmakingPost p) {
        try {
            // Flush of JPA booking/detail inserts is performed before auto-publishing a match.
            Integer valid=jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.PHIEUDATSAN b JOIN dbo.KHACHHANG c ON c.MaKH=b.MaKH
                WHERE b.MaPhieu=? AND c.MaTK=? AND EXISTS (SELECT 1 FROM dbo.CHITIETDATSAN d WHERE d.MaPhieu=b.MaPhieu)
                """,Integer.class,p.getBookingId(),p.getCreatorAccountId());
            if (valid == null || valid != 1) throw new IllegalArgumentException("Kèo phải thuộc một phiếu đặt sân và tài khoản khách hàng có thật");
            String payload=json.writeValueAsString(p);
            int updated=jdbc.update("UPDATE dbo.TIMDOITHU SET MaDoiThu=?,Payload=? WHERE MaKeo=?",p.getOpponentAccountId(),payload,p.getId());
            if(updated==0) jdbc.update("INSERT INTO dbo.TIMDOITHU (MaKeo,MaPhieu,MaDoiThu,Payload) VALUES (?,?,?,?)",p.getId(),p.getBookingId(),p.getOpponentAccountId(),payload);
        } catch (java.io.IOException e) { throw new IllegalStateException("Không thể lưu kèo",e); }
    }
}
