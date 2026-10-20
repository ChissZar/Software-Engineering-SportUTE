package vn.edu.ute.sportute.controller;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.service.MatchmakingService;

@RestController @RequiredArgsConstructor
@RequestMapping("/customer/api/match-notifications")
public class NotificationController {
    private final MatchmakingService matches;
    private final AccountRepository accounts;
    private String id(Authentication a) { return accounts.findByUsernameOrEmail(a.getName(),a.getName()).orElseThrow().getId(); }
    @GetMapping
    public List<Map<String,Object>> list(Authentication a) {
        String id = id(a);
        return matches.getAllPosts().stream().filter(p -> p.getAcceptedAt() != null && matches.isParticipant(p,id)).map(p -> {
            boolean owner = matches.isOwner(p,id);
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id",p.getId()); m.put("read",owner ? p.isCreatorNotificationRead() : p.isOpponentNotificationRead());
            m.put("title",owner ? "Có người nhận kèo " + p.getId() : "Đã ghép kèo " + p.getId());
            m.put("message",owner ? Objects.toString(p.getOpponentMessage(), "") : "Thông tin liên hệ đã có trong Kèo của tôi.");
            m.put("createdAt",p.getAcceptedAt()); return m;
        }).toList();
    }
    @PostMapping("/{postId}/read")
    public Map<String,Boolean> read(@PathVariable String postId, Authentication a) {
        matches.markNotificationRead(postId,id(a)); return Map.of("success",true);
    }
}
