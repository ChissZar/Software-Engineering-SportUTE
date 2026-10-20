package vn.edu.ute.sportute.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.ute.sportute.model.MatchmakingPost;
import vn.edu.ute.sportute.service.MatchmakingService;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@org.springframework.transaction.annotation.Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchmakingServiceImpl implements MatchmakingService {

    @org.springframework.beans.factory.annotation.Autowired
    private vn.edu.ute.sportute.repository.MatchmakingStore store;
    @org.springframework.beans.factory.annotation.Autowired
    private vn.edu.ute.sportute.util.IdGenerator idGenerator;
    private final Optional<JavaMailSender> mailSenderOpt;

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
        for (MatchmakingPost p : store.findAll()) {
            if ("WAITING".equalsIgnoreCase(p.getStatus()) && isPostExpired(p)) {
                p.setStatus("EXPIRED");
                log.info("[MATCHMAKING] Post #{} automatically marked EXPIRED (passed {} {})", p.getId(), p.getMatchDate(), p.getStartTime());
                store.save(p);
            }
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
        return store.findAll().stream()
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
        return store.findAll().stream()
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
        return store.findById(id);
    }

    @Override
    public synchronized MatchmakingPost createPost(MatchmakingPost post) {
        if (post.getId() == null || post.getId().isBlank()) {
            post.setId(idGenerator.generateId("KEO"));
        }
        if (post.getStatus() == null || post.getStatus().isBlank()) {
            post.setStatus("WAITING");
        }
        if (post.getCreatedAt() == null || post.getCreatedAt().isBlank()) {
            post.setCreatedAt(LocalDateTime.now().toString());
        }

        // Thêm vào đầu danh sách
        store.save(post);
        log.info("[MATCHMAKING] Created new match request #{} for {}", post.getId(), post.getSportName());
        return post;
    }

    @Override
    public synchronized MatchmakingPost acceptMatch(String postId, String opponentName, String opponentPhone, String opponentEmail, String message, String accountId) {
        MatchmakingPost post = getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("Kèo thi đấu không tồn tại hoặc đã bị xóa!");
        }
        if (accountId == null || accountId.isBlank()) throw new IllegalArgumentException("Vui lòng đăng nhập để nhận kèo");
        if (isOwner(post, accountId)) throw new IllegalArgumentException("Bạn không thể nhận kèo của chính mình");
        if (isPostExpired(post)) throw new IllegalArgumentException("Kèo đã quá giờ bắt đầu");
        if (!"WAITING".equalsIgnoreCase(post.getStatus())) {
            throw new IllegalStateException("Rất tiếc! Kèo này vừa có đối thủ khác nhận trước rồi.");
        }
        if (opponentName == null || opponentName.isBlank() || opponentPhone == null || opponentPhone.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ họ tên và số điện thoại liên hệ.");
        }

        post.setOpponentAccountId(accountId);
        post.setCreatorNotificationRead(false);
        post.setOpponentNotificationRead(false);
        post.setOpponentName(opponentName.trim());
        post.setOpponentPhone(opponentPhone.trim());
        post.setOpponentEmail(opponentEmail != null ? opponentEmail.trim() : "");
        post.setOpponentMessage(message != null ? message.trim() : "Rất vui được giao lưu thể thao cùng bạn!");
        post.setAcceptedAt(LocalDateTime.now().toString());
        post.setStatus("MATCHED");

        store.save(post);
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

        store.save(post);
        log.warn("[MATCHMAKING WARNING] Match #{} reported for violation by {}: {}", post.getId(), reporterName, reason);
        return true;
    }

    @Override
    public synchronized boolean cancelPost(String postId, String requester) {
        MatchmakingPost post = getPostById(postId);
        if (post == null) return false;
        post.setStatus("CANCELLED");
        store.save(post);
        log.info("[MATCHMAKING] Match #{} was cancelled by {}", postId, requester);
        return true;
    }

    @Override
    public synchronized boolean cancelPostByBookingId(String bookingId, String requester) {
        if (bookingId == null || bookingId.isBlank()) return false;
        boolean modified = false;
        for (MatchmakingPost post : store.findAll()) {
            if (bookingId.equalsIgnoreCase(post.getBookingId())) {
                post.setStatus("CANCELLED");
                log.info("[MATCHMAKING] Match #{} cancelled because booking #{} was cancelled by {}", post.getId(), bookingId, requester);
                store.save(post);
                modified = true;
            }
        }

        return modified;
    }

    public boolean isOwner(MatchmakingPost p, String id) {
        return p != null && id != null && id.equals(p.getCreatorAccountId());
    }
    public boolean isParticipant(MatchmakingPost p, String id) {
        return isOwner(p,id) || (p != null && id != null && id.equals(p.getOpponentAccountId()));
    }
    public MatchmakingPost visibleTo(MatchmakingPost p, String id) {
        boolean participant = isParticipant(p,id);
        var copy = p.toBuilder().owner(isOwner(p,id)).participant(participant).build();
        copy.setCreatorAccountId(null); copy.setOpponentAccountId(null);
        if (!participant) {
            copy.setCreatorPhone("Chỉ hiện sau khi ghép"); copy.setCreatorEmail(null);
            copy.setOpponentPhone(null); copy.setOpponentEmail(null); copy.setOpponentMessage(null);
            copy.setReportReason(null); copy.setReportedBy(null); copy.setBookingId(null);
        }
        return copy;
    }
    public synchronized void markNotificationRead(String postId, String accountId) {
        var p = getPostById(postId);
        if (!isParticipant(p,accountId)) throw new IllegalArgumentException("Không có quyền xem thông báo");
        if (isOwner(p,accountId)) p.setCreatorNotificationRead(true); else p.setOpponentNotificationRead(true);
        store.save(p);
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
                    org.springframework.web.util.HtmlUtils.htmlEscape(java.util.Objects.toString(post.getCreatorName(), "")),
                    org.springframework.web.util.HtmlUtils.htmlEscape(java.util.Objects.toString(post.getCourtName(), "")), org.springframework.web.util.HtmlUtils.htmlEscape(java.util.Objects.toString(post.getSportName(), "")),
                    post.getStartTime(), post.getEndTime(), post.getMatchDate(),
                    org.springframework.web.util.HtmlUtils.htmlEscape(java.util.Objects.toString(post.getOpponentName(), "")),
                    org.springframework.web.util.HtmlUtils.htmlEscape(java.util.Objects.toString(post.getOpponentPhone(), "")),
                    org.springframework.web.util.HtmlUtils.htmlEscape(post.getOpponentMessage() != null ? post.getOpponentMessage() : "Sẵn sàng thi đấu!")
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
