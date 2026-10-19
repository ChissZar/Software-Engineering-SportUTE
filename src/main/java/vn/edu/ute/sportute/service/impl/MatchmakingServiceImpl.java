package vn.edu.ute.sportute.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.ute.sportute.model.MatchmakingPost;
import vn.edu.ute.sportute.service.MatchmakingService;

import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingServiceImpl implements MatchmakingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File dataFile = new File("data/matchmaking.json");
    private final List<MatchmakingPost> postStore = new CopyOnWriteArrayList<>();
    private final Optional<JavaMailSender> mailSenderOpt;

    @PostConstruct
    public void init() {
        try {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            if (dataFile.exists()) {
                List<MatchmakingPost> loaded = objectMapper.readValue(dataFile, new TypeReference<List<MatchmakingPost>>() {});
                if (loaded != null && !loaded.isEmpty()) {
                    postStore.addAll(loaded);
                    log.info("[MATCHMAKING] Loaded {} match posts from disk", postStore.size());
                    return;
                }
            }

            // Nếu file chưa có dữ liệu, khởi tạo 3 kèo mẫu phong phú, sinh động
            seedInitialPosts();
            saveToDisk();
            log.info("[MATCHMAKING] Initialized with {} sample match posts", postStore.size());

        } catch (Exception e) {
            log.error("[MATCHMAKING] Failed to load match posts: {}", e.getMessage());
            if (postStore.isEmpty()) {
                seedInitialPosts();
            }
        }
    }

    private void seedInitialPosts() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        postStore.add(MatchmakingPost.builder()
                .id("KEO-101")
                .bookingId("DS0101")
                .sportName("Bóng đá")
                .courtName("Sân bóng đá 1")
                .courtId("S01")
                .matchDate(today.format(dFmt))
                .startTime("19:00")
                .endTime("21:00")
                .durationMinutes(120)
                .creatorName("Nguyễn Văn Triết")
                .creatorPhone("0901 234 567")
                .creatorEmail("fifaminchiss@gmail.com")
                .skillLevel("Trung bình / Phong trào")
                .teamSize("5 vs 5")
                .message("Đội mình gồm 6-7 bạn sinh viên UTE cọ xát cuối tuần, tìm đội đá giao lưu vui vẻ, nói không với va chạm mạnh. Tiền sân mình đã thanh toán, hai bên share 50% tiền sân nhé!")
                .status("WAITING")
                .createdAt(LocalDateTime.now().minusHours(2).toString())
                .build());

        postStore.add(MatchmakingPost.builder()
                .id("KEO-102")
                .bookingId("DS0102")
                .sportName("Cầu lông")
                .courtName("Sân cầu lông 2")
                .courtId("S04")
                .matchDate(tomorrow.format(dFmt))
                .startTime("18:00")
                .endTime("19:30")
                .durationMinutes(90)
                .creatorName("Lê Hoàng Yến")
                .creatorPhone("0988 765 432")
                .creatorEmail("hoangyen.ute@gmail.com")
                .skillLevel("Mới chơi / Giao lưu vui vẻ")
                .teamSize("Đánh đôi")
                .message("Tìm 1 cặp đôi nam nữ hoặc đôi nữ cùng tập luyện nâng cao tay vợt. Tiền sân mình bao trọn gói 100%, đối thủ chỉ cần đến chơi nghiêm túc và mang vợt cá nhân nha!")
                .status("WAITING")
                .createdAt(LocalDateTime.now().minusHours(5).toString())
                .build());

        postStore.add(MatchmakingPost.builder()
                .id("KEO-103")
                .bookingId("DS0103")
                .sportName("Pickleball")
                .courtName("Sân pickleball 1")
                .courtId("S07")
                .matchDate(tomorrow.format(dFmt))
                .startTime("17:30")
                .endTime("19:00")
                .durationMinutes(90)
                .creatorName("Phạm Quốc Bảo")
                .creatorPhone("0933 112 233")
                .creatorEmail("quocbao.sporthub@gmail.com")
                .skillLevel("Khá / Bán chuyên")
                .teamSize("Đánh đôi")
                .message("Kèo Pickleball đôi nam chuẩn bị thi giải nội bộ trung tâm. Cần tìm 2 bạn đánh bóng bền, có kinh nghiệm rally. Hai bên share tiền nước giải khát F&B vui vẻ sau trận.")
                .status("WAITING")
                .createdAt(LocalDateTime.now().minusHours(8).toString())
                .build());
    }

    private synchronized void saveToDisk() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, postStore);
        } catch (Exception e) {
            log.error("[MATCHMAKING] Error saving to disk: {}", e.getMessage());
        }
    }

    public boolean isPostExpired(MatchmakingPost post) {
        if (post == null || post.getMatchDate() == null || post.getStartTime() == null) {
            return false;
        }
        try {
            LocalDate mDate = LocalDate.parse(post.getMatchDate().trim());
            String sTimeStr = post.getStartTime().trim();
            if (sTimeStr.length() > 5) sTimeStr = sTimeStr.substring(0, 5);
            LocalTime sTime = LocalTime.parse(sTimeStr);
            LocalDateTime matchStart = LocalDateTime.of(mDate, sTime);

            // Kèo hết hạn khi thời điểm hiện tại đã vượt qua giờ bắt đầu thi đấu
            return LocalDateTime.now().isAfter(matchStart);
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized void updateExpiredPosts() {
        boolean changed = false;
        for (MatchmakingPost p : postStore) {
            if ("WAITING".equalsIgnoreCase(p.getStatus()) && isPostExpired(p)) {
                p.setStatus("EXPIRED");
                log.info("[MATCHMAKING] Post #{} automatically marked EXPIRED (passed {} {})", p.getId(), p.getMatchDate(), p.getStartTime());
                changed = true;
            }
        }
        if (changed) {
            saveToDisk();
        }
    }

    private int getStatusRank(String status) {
        if ("WAITING".equalsIgnoreCase(status)) return 1;
        if ("MATCHED".equalsIgnoreCase(status)) return 2;
        if ("EXPIRED".equalsIgnoreCase(status)) return 3;
        return 4; // CANCELLED
    }

    @Override
    public List<MatchmakingPost> getAllPosts() {
        updateExpiredPosts();
        return postStore.stream()
                .sorted((a, b) -> {
                    int rankA = getStatusRank(a.getStatus());
                    int rankB = getStatusRank(b.getStatus());
                    if (rankA != rankB) {
                        return Integer.compare(rankA, rankB);
                    }
                    return b.getId().compareTo(a.getId());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchmakingPost> getActivePosts(String sport, String date) {
        updateExpiredPosts();
        return postStore.stream()
                .filter(p -> "WAITING".equalsIgnoreCase(p.getStatus()))
                .filter(p -> !isPostExpired(p))
                .filter(p -> {
                    if (sport == null || sport.isBlank() || "ALL".equalsIgnoreCase(sport) || "Tất cả".equalsIgnoreCase(sport)) {
                        return true;
                    }
                    return p.getSportName() != null && p.getSportName().equalsIgnoreCase(sport.trim());
                })
                .filter(p -> {
                    if (date == null || date.isBlank()) return true;
                    return p.getMatchDate() != null && p.getMatchDate().equals(date.trim());
                })
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public MatchmakingPost getPostById(String id) {
        if (id == null) return null;
        return postStore.stream()
                .filter(p -> p.getId().equalsIgnoreCase(id.trim()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public synchronized MatchmakingPost createPost(MatchmakingPost post) {
        if (post.getId() == null || post.getId().isBlank()) {
            post.setId("KEO-" + (100 + postStore.size() + 1));
        }
        if (post.getStatus() == null || post.getStatus().isBlank()) {
            post.setStatus("WAITING");
        }
        if (post.getCreatedAt() == null || post.getCreatedAt().isBlank()) {
            post.setCreatedAt(LocalDateTime.now().toString());
        }

        // Thêm vào đầu danh sách
        postStore.add(0, post);
        saveToDisk();
        log.info("[MATCHMAKING] Created new match request #{} for {}", post.getId(), post.getSportName());
        return post;
    }

    @Override
    public synchronized MatchmakingPost acceptMatch(String postId, String opponentName, String opponentPhone, String opponentEmail, String message) {
        MatchmakingPost post = getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("Kèo thi đấu không tồn tại hoặc đã bị xóa!");
        }
        if (!"WAITING".equalsIgnoreCase(post.getStatus())) {
            throw new IllegalStateException("Rất tiếc! Kèo này vừa có đối thủ khác nhận trước rồi.");
        }
        if (opponentName == null || opponentName.isBlank() || opponentPhone == null || opponentPhone.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ họ tên và số điện thoại liên hệ.");
        }

        post.setOpponentName(opponentName.trim());
        post.setOpponentPhone(opponentPhone.trim());
        post.setOpponentEmail(opponentEmail != null ? opponentEmail.trim() : "");
        post.setOpponentMessage(message != null ? message.trim() : "Rất vui được giao lưu thể thao cùng bạn!");
        post.setAcceptedAt(LocalDateTime.now().toString());
        post.setStatus("MATCHED");

        saveToDisk();
        log.info("[MATCHMAKING] Match #{} was accepted by {} ({})", post.getId(), opponentName, opponentPhone);

        // Gửi email thông báo cho người tạo sân
        sendEmailNotification(post);

        return post;
    }

    @Override
    public synchronized boolean reportViolation(String postId, String reason, String reporterName) {
        MatchmakingPost post = getPostById(postId);
        if (post == null) return false;

        post.setReported(true);
        post.setReportReason(reason != null && !reason.isBlank() ? reason.trim() : "Đối thủ không đến thi đấu (bùng kèo / không liên lạc được)");
        post.setReportedBy(reporterName != null ? reporterName.trim() : "Người tạo sân");
        post.setReportedAt(LocalDateTime.now().toString());

        saveToDisk();
        log.warn("[MATCHMAKING WARNING] Match #{} reported for violation by {}: {}", post.getId(), reporterName, reason);
        return true;
    }

    @Override
    public synchronized boolean cancelPost(String postId, String requester) {
        MatchmakingPost post = getPostById(postId);
        if (post == null) return false;
        post.setStatus("CANCELLED");
        saveToDisk();
        log.info("[MATCHMAKING] Match #{} was cancelled by {}", postId, requester);
        return true;
    }

    @Override
    public synchronized boolean cancelPostByBookingId(String bookingId, String requester) {
        if (bookingId == null || bookingId.isBlank()) return false;
        boolean modified = false;
        for (MatchmakingPost post : postStore) {
            if (bookingId.equalsIgnoreCase(post.getBookingId())) {
                post.setStatus("CANCELLED");
                log.info("[MATCHMAKING] Match #{} cancelled because booking #{} was cancelled by {}", post.getId(), bookingId, requester);
                modified = true;
            }
        }
        if (modified) {
            saveToDisk();
        }
        return modified;
    }

    private void sendEmailNotification(MatchmakingPost post) {
        if (mailSenderOpt.isEmpty() || post.getCreatorEmail() == null || post.getCreatorEmail().isBlank()) {
            return;
        }

        try {
            JavaMailSender sender = mailSenderOpt.get();
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setTo(post.getCreatorEmail());
            helper.setSubject("🎉 [SportHub] Đã có đối thủ nhận kèo trận đấu của bạn (" + post.getCourtName() + ")");

            String html = String.format(
                    "<div style='font-family:sans-serif;max-width:600px;margin:auto;padding:24px;border:1px solid #e2e8f0;border-radius:12px;background:#ffffff;'>" +
                    "<div style='text-align:center;margin-bottom:20px;'>" +
                    "<h2 style='color:#059669;margin:0;'>🎉 KÈO THỂ THAO ĐÃ ĐƯỢC GHÉP ĐỐI!</h2>" +
                    "<p style='color:#64748b;font-size:14px;margin-top:6px;'>Trung tâm Thể thao SportHub xin thông báo</p>" +
                    "</div>" +
                    "<p>Xin chào <strong>%s</strong>,</p>" +
                    "<p>Trận đấu bạn đăng tìm đối thủ trên <strong>Bảng Ghép Kèo SportHub</strong> đã có người nhận kèo tham gia:</p>" +
                    "<div style='background:#f8fafc;padding:16px;border-radius:8px;margin:16px 0;line-height:1.7;'>" +
                    "🏟️ <strong>Sân:</strong> %s (%s)<br>" +
                    "⏰ <strong>Thời gian:</strong> %s – %s (%s)<br>" +
                    "👤 <strong>Đối thủ nhận kèo:</strong> <span style='color:#0f172a;font-weight:700;'>%s</span><br>" +
                    "📞 <strong>Số điện thoại / Zalo:</strong> <span style='color:#059669;font-weight:700;font-size:16px;'>%s</span><br>" +
                    "💬 <strong>Lời nhắn đối thủ:</strong> <em>\"%s\"</em>" +
                    "</div>" +
                    "<div style='background:#fef3c7;border-left:4px solid #f59e0b;padding:12px;font-size:13px;color:#92400e;border-radius:4px;margin:16px 0;'>" +
                    "<strong>Lưu ý:</strong> Vui lòng liên hệ với đối thủ qua SĐT/Zalo ở trên để xác nhận lại thời gian và thỏa thuận chi phí giao lưu nếu có. Chúc bạn có trận đấu thể thao vui khỏe!" +
                    "</div>" +
                    "<p style='color:#64748b;font-size:13px;text-align:center;margin-top:24px;'>SportHub — Hệ thống quản lý & đặt sân thể thao thông minh</p>" +
                    "</div>",
                    post.getCreatorName(),
                    post.getCourtName(), post.getSportName(),
                    post.getStartTime(), post.getEndTime(), post.getMatchDate(),
                    post.getOpponentName(),
                    post.getOpponentPhone(),
                    post.getOpponentMessage() != null ? post.getOpponentMessage() : "Sẵn sàng thi đấu!"
            );

            helper.setText(html, true);

            // Gửi bất đồng bộ trong thread riêng để không block request
            new Thread(() -> {
                try {
                    sender.send(mime);
                    log.info("[MATCHMAKING EMAIL] Sent notification email to {}", post.getCreatorEmail());
                } catch (Exception ex) {
                    log.warn("[MATCHMAKING EMAIL] Failed to send email to {}: {}", post.getCreatorEmail(), ex.getMessage());
                }
            }).start();

        } catch (Exception e) {
            log.warn("[MATCHMAKING EMAIL] Error preparing notification email: {}", e.getMessage());
        }
    }
}
