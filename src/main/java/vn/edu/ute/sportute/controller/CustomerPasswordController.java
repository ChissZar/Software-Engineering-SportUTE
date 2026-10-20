package vn.edu.ute.sportute.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.sportute.service.AuthService;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CustomerPasswordController {
    private final AuthService authService;

    @PostMapping("/customer/password/change")
    public ResponseEntity<Map<String,Object>> changePassword(Authentication authentication,
            @RequestParam String currentPassword, @RequestParam String newPassword,
            @RequestParam String confirmPassword) {
        if (authentication == null)
            return ResponseEntity.status(401).body(Map.of("success",false,"message","Vui lòng đăng nhập lại"));
        try {
            authService.changePassword(authentication.getName(),currentPassword,newPassword,confirmPassword);
            return ResponseEntity.ok(Map.of("success",true,"message","Đã đổi mật khẩu thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success",false,"message",e.getMessage()));
        }
    }
}
