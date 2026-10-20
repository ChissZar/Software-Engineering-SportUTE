package vn.edu.ute.sportute.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.model.MatchmakingPost;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.service.MatchmakingService;
import vn.edu.ute.sportute.service.SportService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/customer/community")
@RequiredArgsConstructor
@Slf4j
public class CommunityController {

    private final MatchmakingService matchmakingService;
    private final SportService sportService;
    private final AdminBookingService adminBookingService;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    private Customer findCustomer(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return null;
        String username = authentication.getName();
        Optional<Account> acc = accountRepository.findByUsernameOrEmail(username, username);
        if (acc.isPresent()) {
            Optional<Customer> cust = customerRepository.findByAccount_Id(acc.get().getId());
            if (cust.isPresent()) return cust.get();
        }
        Optional<Customer> byPhone = customerRepository.findByPhone(username);
        if (byPhone.isPresent()) return byPhone.get();

        Optional<Customer> byEmail = customerRepository.findByEmail(username);
        return byEmail.orElse(null);
    }

    private String accountId(Authentication auth) {
        if (auth == null) return null;
        return accountRepository.findByUsernameOrEmail(auth.getName(),auth.getName()).map(Account::getId).orElse(null);
    }
    private String requireAccount(Authentication auth) {
        String id = accountId(auth);
        if (id == null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        return id;
    }

    @GetMapping("/mine")
    public String mine(Model model, Authentication auth) {
        String id = requireAccount(auth);
        model.addAttribute("posts", matchmakingService.getAllPosts().stream().filter(p -> matchmakingService.isParticipant(p,id)).map(p -> matchmakingService.visibleTo(p,id)).toList());
        return "customer/my-matches";
    }

    @GetMapping({"", "/"})
    public String communityPage(@RequestParam(value = "sport", required = false) String sport,
                                @RequestParam(value = "date", required = false) String date,
                                Model model,
                                Authentication authentication) {
        Customer customer = findCustomer(authentication);
        String displayName = (customer != null && customer.getFullName() != null)
                ? customer.getFullName() : (authentication != null ? authentication.getName() : "Khách thể thao");

        model.addAttribute("currentUserName", displayName);
        model.addAttribute("customer", customer);
        model.addAttribute("sports", sportService.getAllSports());

        // Chỉ hiển thị các kèo đang chờ đối thủ và chưa quá giờ thi đấu
        List<MatchmakingPost> activePosts = matchmakingService.getActivePosts(sport, date);
        model.addAttribute("allPosts", activePosts.stream().map(p -> matchmakingService.visibleTo(p,accountId(authentication))).toList());

        long waitingCount = activePosts.size();
        model.addAttribute("waitingCount", waitingCount);

        model.addAttribute("selectedSport", sport != null ? sport : "ALL");
        model.addAttribute("selectedDate", date != null ? date : "");

        return "customer/community";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<MatchmakingPost>> getPostsApi(
            @RequestParam(value = "sport", required = false) String sport,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "all", required = false, defaultValue = "false") boolean all, Authentication authentication) {
        if (all) {
            return ResponseEntity.ok(matchmakingService.getAllPosts().stream().map(p -> matchmakingService.visibleTo(p,accountId(authentication))).toList());
        }
        return ResponseEntity.ok(matchmakingService.getActivePosts(sport, date).stream().map(p -> matchmakingService.visibleTo(p,accountId(authentication))).toList());
    }

    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public ResponseEntity<MatchmakingPost> getPostDetailApi(@PathVariable("id") String id, Authentication authentication) {
        MatchmakingPost post = matchmakingService.getPostById(id);
        if (post == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(matchmakingService.visibleTo(post,accountId(authentication)));
    }

    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPostApi(
            @RequestBody MatchmakingPost post,
            Authentication authentication) {
        Map<String,String> body = new HashMap<>();
        body.put("bookingId",post.getBookingId()); body.put("skillLevel",post.getSkillLevel());
        body.put("teamSize",post.getTeamSize()); body.put("message",post.getMessage());
        return createFromBookingApi(body, authentication);
    }

    @PostMapping("/api/accept")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> acceptMatchApi(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String postId = body.get("postId");
            String opponentName = body.get("opponentName");
            String opponentPhone = body.get("opponentPhone");
            String opponentEmail = body.get("opponentEmail");
            String message = body.get("message");

            Customer customer = findCustomer(authentication);
            if (customer != null) {
                if (opponentName == null || opponentName.isBlank()) opponentName = customer.getFullName();
                if (opponentPhone == null || opponentPhone.isBlank()) opponentPhone = customer.getPhone();
                if (opponentEmail == null || opponentEmail.isBlank()) opponentEmail = customer.getEmail();
            }

            MatchmakingPost matched = matchmakingService.acceptMatch(postId, opponentName, opponentPhone, opponentEmail, message, requireAccount(authentication));
            resp.put("success", true);
            resp.put("message", "Nhận kèo thành công! Hãy lưu lại thông tin liên hệ và đến sân đúng giờ bạn nhé.");
            resp.put("post", matchmakingService.visibleTo(matched,accountId(authentication)));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/api/report")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reportViolationApi(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String postId = body.get("postId");
            String reason = body.get("reason");

            Customer customer = findCustomer(authentication);
            String reporter = (customer != null && customer.getFullName() != null)
                    ? customer.getFullName() : (authentication != null ? authentication.getName() : "Người dùng");

            if (!matchmakingService.isParticipant(matchmakingService.getPostById(postId), requireAccount(authentication)))
                throw new IllegalArgumentException("Chỉ người tham gia mới được báo cáo kèo");
            boolean ok = matchmakingService.reportViolation(postId, reason, reporter);
            if (ok) {
                resp.put("success", true);
                resp.put("message", "Hệ thống đã tiếp nhận báo cáo. Đội ngũ SportHub đã ghi nhận vi phạm và gửi cảnh cáo tới đối thủ!");
            } else {
                resp.put("success", false);
                resp.put("message", "Không tìm thấy thông tin kèo thi đấu.");
            }
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/api/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelPostApi(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String postId = body.get("postId");
            if (postId == null || postId.isBlank()) {
                resp.put("success", false);
                resp.put("message", "Mã bài đăng không hợp lệ.");
                return ResponseEntity.badRequest().body(resp);
            }
            MatchmakingPost post = matchmakingService.getPostById(postId);
            if (post == null) {
                resp.put("success", false);
                resp.put("message", "Không tìm thấy bài đăng tìm đối.");
                return ResponseEntity.badRequest().body(resp);
            }

            Customer customer = findCustomer(authentication);
            String requester = (customer != null && customer.getFullName() != null)
                    ? customer.getFullName() : (authentication != null ? authentication.getName() : "Người dùng");

            if (!matchmakingService.isOwner(post, requireAccount(authentication))) throw new IllegalArgumentException("Chỉ người đăng mới được hủy kèo");
            boolean ok = matchmakingService.cancelPost(postId, requester);
            if (ok) {
                resp.put("success", true);
                resp.put("message", "Đã gỡ bài đăng tìm đối thủ thành công.");
            } else {
                resp.put("success", false);
                resp.put("message", "Không thể gỡ bài đăng này.");
            }
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/api/create-from-booking")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createFromBookingApi(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String bookingId = body.get("bookingId");
            if (bookingId == null || bookingId.isBlank()) {
                resp.put("success", false);
                resp.put("message", "Mã phiếu đặt sân không hợp lệ.");
                return ResponseEntity.badRequest().body(resp);
            }

            Customer customer = findCustomer(authentication);
            vn.edu.ute.sportute.dto.request.AdminBookingDTO booking = adminBookingService.getBookingById(bookingId);
            if (booking == null) {
                resp.put("success", false);
                resp.put("message", "Không tìm thấy thông tin phiếu đặt sân #" + bookingId);
                return ResponseEntity.badRequest().body(resp);
            }

            String creatorId = requireAccount(authentication);
            if (customer == null || !customer.getId().equals(booking.getCustomerId())) throw new IllegalArgumentException("Bạn không sở hữu phiếu này");
            // Kiểm tra trạng thái phiếu
            if ("Đã hủy".equalsIgnoreCase(booking.getStatus())) {
                resp.put("success", false);
                resp.put("message", "Phiếu đặt sân này đã bị hủy, không thể đăng tin tìm đối thủ.");
                return ResponseEntity.badRequest().body(resp);
            }

            // Kiểm tra thời gian thi đấu: phải trước giờ bắt đầu
            if (booking.getBookingDate() != null && booking.getStartTime() != null) {
                java.time.LocalDateTime matchStart = java.time.LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
                if (java.time.LocalDateTime.now().isAfter(matchStart)) {
                    resp.put("success", false);
                    resp.put("message", "Trận đấu đã quá giờ bắt đầu thi đấu, không thể tìm đối thủ.");
                    return ResponseEntity.badRequest().body(resp);
                }
            }

            // Kiểm tra xem đã có bài đăng cho phiếu này chưa
            boolean postExists = matchmakingService.getAllPosts().stream()
                    .anyMatch(p -> bookingId.equalsIgnoreCase(p.getBookingId()) && !"CANCELLED".equalsIgnoreCase(p.getStatus()));
            if (postExists) {
                resp.put("success", false);
                resp.put("message", "Phiếu đặt sân #" + bookingId + " đã có bài đăng tìm đối thủ trên hệ thống.");
                return ResponseEntity.badRequest().body(resp);
            }

            String skillLevel = body.get("skillLevel");
            String teamSize = body.get("teamSize");
            String message = body.get("message");

            long duration = 120;
            if (booking.getStartTime() != null && booking.getEndTime() != null) {
                duration = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
                if (duration <= 0) duration = 120;
            }

            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

            MatchmakingPost post = MatchmakingPost.builder()
                    .creatorAccountId(creatorId)
                    .bookingId(booking.getId())
                    .sportName(booking.getSportName() != null ? booking.getSportName() : "Thể thao")
                    .courtName(booking.getCourtName())
                    .courtId(booking.getCourtId())
                    .matchDate(booking.getBookingDate() != null ? booking.getBookingDate().format(dateFmt) : "")
                    .startTime(booking.getStartTime() != null ? booking.getStartTime().format(timeFmt) : "")
                    .endTime(booking.getEndTime() != null ? booking.getEndTime().format(timeFmt) : "")
                    .durationMinutes((int) duration)
                    .creatorName((customer != null && customer.getFullName() != null) ? customer.getFullName() : booking.getCustomerName())
                    .creatorPhone((customer != null && customer.getPhone() != null) ? customer.getPhone() : booking.getCustomerPhone())
                    .creatorEmail((customer != null && customer.getEmail() != null) ? customer.getEmail() : booking.getCustomerEmail())
                    .skillLevel(skillLevel != null && !skillLevel.isBlank() ? skillLevel : "Trung bình / Phong trào")
                    .teamSize(teamSize != null && !teamSize.isBlank() ? teamSize : "Giao lưu")
                    .message(message != null && !message.isBlank() ? message : "Giao lưu vui vẻ, share 50% tiền sân!")
                    .status("WAITING")
                    .build();

            MatchmakingPost created = matchmakingService.createPost(post);
            resp.put("success", true);
            resp.put("message", "Đăng tin tìm đối thủ thành công cho phiếu #" + bookingId + "! Kèo đã xuất hiện trên Sàn Ghép Kèo.");
            resp.put("post", matchmakingService.visibleTo(created,accountId(authentication)));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("[CommunityController] Lỗi tạo kèo từ phiếu đặt: {}", e.getMessage());
            resp.put("success", false);
            resp.put("message", "Không thể tạo bài đăng tìm đối: " + e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }
}

