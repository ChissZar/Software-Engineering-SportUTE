package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.ForgotPasswordRequest;
import vn.edu.ute.sportute.dto.request.LoginRequest;
import vn.edu.ute.sportute.dto.request.RegisterRequest;
import vn.edu.ute.sportute.dto.request.ResetPasswordRequest;
import vn.edu.ute.sportute.dto.request.VerifyOtpRequest;
import vn.edu.ute.sportute.entity.Customer;
import vn.edu.ute.sportute.enums.OtpType;
import vn.edu.ute.sportute.repository.AccountRepository;
import vn.edu.ute.sportute.repository.CustomerRepository;
import vn.edu.ute.sportute.service.AuthService;
import vn.edu.ute.sportute.service.EmailService;
import vn.edu.ute.sportute.service.OtpService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AuthController(AuthService authService,
                          OtpService otpService,
                          EmailService emailService,
                          AccountRepository accountRepository,
                          CustomerRepository customerRepository) {
        this.authService = authService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    // ==========================================
    // 1. ĐĂNG NHẬP
    // ==========================================
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "auth/login";
    }

    // ==========================================
    // 2. ĐĂNG KÝ VỚI XÁC THỰC OTP
    // ==========================================
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        // Kiểm tra khớp mật khẩu
        if (request.getConfirmPassword() == null || !request.getConfirmPassword().equals(request.getPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không trùng khớp!");
            return "auth/register";
        }

        // Kiểm tra trùng lặp email và số điện thoại
        if (accountRepository.existsByEmail(request.getEmail())) {
            result.rejectValue("email", "error.email", "Địa chỉ email này đã được đăng ký tài khoản!");
            return "auth/register";
        }

        // Kiểm tra số điện thoại:
        // Nếu số điện thoại đã thuộc về một khách hàng ĐÃ CÓ tài khoản thì chặn lại
        Optional<Customer> existingByPhone = customerRepository.findByPhone(request.getPhone());
        if (existingByPhone.isPresent() && existingByPhone.get().getAccount() != null) {
            result.rejectValue("phone", "error.phone", "Số điện thoại này đã được liên kết với một tài khoản đang hoạt động! Vui lòng đăng nhập hoặc dùng chức năng quên mật khẩu.");
            return "auth/register";
        }

        // Kiểm tra email trong hồ sơ khách hàng: nếu email đã thuộc về một khách hàng khác thì báo lỗi
        Optional<Customer> existingByEmail = customerRepository.findByEmail(request.getEmail());
        if (existingByEmail.isPresent()) {
            if (existingByPhone.isEmpty() || !existingByEmail.get().getId().equals(existingByPhone.get().getId())) {
                result.rejectValue("email", "error.email", "Địa chỉ email này đã được sử dụng bởi một khách hàng khác!");
                return "auth/register";
            }
        }

        try {
            // Tạo OTP & lưu thông tin đăng ký chờ xác thực
            String otpCode = otpService.generateAndSaveRegisterOtp(request);
            emailService.sendRegisterOtpEmail(request.getEmail(), request.getFullName(), otpCode);

            redirectAttributes.addFlashAttribute("infoMessage", 
                    "Mã xác thực OTP đã được gửi đến email " + request.getEmail() + ". Vui lòng nhập mã để hoàn tất đăng ký.");
            return "redirect:/register/verify-otp?email=" + URLEncoder.encode(request.getEmail(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            result.reject("registerError", "Đã xảy ra lỗi trong quá trình khởi tạo mã xác thực: " + e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/register/verify-otp")
    public String showRegisterOtpForm(@RequestParam(value = "email", required = false) String email,
                                      @RequestParam(value = "otp", required = false) String otp,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            return "redirect:/register";
        }

        RegisterRequest pending = otpService.getPendingRegistration(email);
        if (pending == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Yêu cầu đăng ký đã hết hạn hoặc không tồn tại. Vui lòng đăng ký lại.");
            return "redirect:/register";
        }

        if (!model.containsAttribute("verifyOtpRequest")) {
            VerifyOtpRequest req = new VerifyOtpRequest();
            req.setEmail(email);
            if (otp != null && !otp.isBlank()) {
                req.setOtp(otp.trim());
            }
            model.addAttribute("verifyOtpRequest", req);
        }

        model.addAttribute("email", email);
        return "auth/verify-register-otp";
    }

    @PostMapping("/register/verify-otp")
    public String processRegisterOtp(@Valid @ModelAttribute("verifyOtpRequest") VerifyOtpRequest request,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("email", request.getEmail());
            return "auth/verify-register-otp";
        }

        String email = request.getEmail();
        boolean isValid = otpService.verifyRegisterOtp(email, request.getOtp());
        if (!isValid) {
            result.rejectValue("otp", "error.otp", "Mã OTP không chính xác hoặc đã hết hạn (5 phút)!");
            model.addAttribute("email", email);
            return "auth/verify-register-otp";
        }

        RegisterRequest pending = otpService.getPendingRegistration(email);
        if (pending == null) {
            result.reject("error.expired", "Phiên đăng ký đã hết hạn. Vui lòng thao tác lại từ đầu.");
            return "auth/verify-register-otp";
        }

        try {
            authService.registerCustomer(pending);
            otpService.clearOtp(email, OtpType.REGISTER);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Xác thực OTP thành công! Tài khoản của bạn đã được kích hoạt (lịch sử đặt sân tại quầy trước đây nếu có đã được đồng bộ tự động). Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            result.reject("registerError", "Lỗi tạo tài khoản: " + e.getMessage());
            model.addAttribute("email", email);
            return "auth/verify-register-otp";
        }
    }

    @PostMapping("/register/resend-otp")
    public String resendRegisterOtp(@RequestParam("email") String email,
                                    RedirectAttributes redirectAttributes) {
        RegisterRequest pending = otpService.getPendingRegistration(email);
        if (pending != null) {
            String newOtp = otpService.generateAndSaveRegisterOtp(pending);
            emailService.sendRegisterOtpEmail(email, pending.getFullName(), newOtp);
            redirectAttributes.addFlashAttribute("infoMessage", "Mã OTP mới đã được gửi lại thành công tới " + email);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên đăng ký không còn tồn tại. Vui lòng đăng ký lại.");
            return "redirect:/register";
        }
        return "redirect:/register/verify-otp?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    // ==========================================
    // 3. QUÊN MẬT KHẨU VỚI XÁC THỰC OTP
    // ==========================================
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        if (!model.containsAttribute("forgotPasswordRequest")) {
            model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
                                        BindingResult result,
                                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/forgot-password";
        }

        String email = request.getEmail().trim();
        if (!accountRepository.existsByEmail(email)) {
            result.rejectValue("email", "error.email", "Không tìm thấy tài khoản nào liên kết với email này!");
            return "auth/forgot-password";
        }

        try {
            String otpCode = otpService.generateAndSaveForgotPasswordOtp(email);
            emailService.sendForgotPasswordOtpEmail(email, otpCode);

            redirectAttributes.addFlashAttribute("infoMessage", 
                    "Mã xác thực OTP đặt lại mật khẩu đã được gửi tới " + email + ". Vui lòng nhập mã để tạo mật khẩu mới.");
            return "redirect:/forgot-password/verify-otp?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
        } catch (Exception e) {
            result.reject("forgotError", "Lỗi gửi mã OTP: " + e.getMessage());
            return "auth/forgot-password";
        }
    }

    @GetMapping("/forgot-password/verify-otp")
    public String showForgotPasswordOtpForm(@RequestParam(value = "email", required = false) String email,
                                            @RequestParam(value = "otp", required = false) String otp,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            return "redirect:/forgot-password";
        }

        if (!model.containsAttribute("resetPasswordRequest")) {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setEmail(email);
            if (otp != null && !otp.isBlank()) {
                req.setOtp(otp.trim());
            }
            model.addAttribute("resetPasswordRequest", req);
        }

        model.addAttribute("email", email);
        return "auth/verify-forgot-otp";
    }

    @PostMapping("/forgot-password/verify-otp")
    public String processResetPassword(@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
                                       BindingResult result,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("email", request.getEmail());
            return "auth/verify-forgot-otp";
        }

        // Kiểm tra khớp mật khẩu
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không trùng khớp!");
            model.addAttribute("email", request.getEmail());
            return "auth/verify-forgot-otp";
        }

        // Xác thực mã OTP
        String email = request.getEmail();
        boolean isValid = otpService.verifyForgotPasswordOtp(email, request.getOtp());
        if (!isValid) {
            result.rejectValue("otp", "error.otp", "Mã xác thực OTP không chính xác hoặc đã hết hiệu lực (5 phút)!");
            model.addAttribute("email", email);
            return "auth/verify-forgot-otp";
        }

        try {
            authService.resetPassword(email, request.getNewPassword());
            otpService.clearOtp(email, OtpType.FORGOT_PASSWORD);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception e) {
            result.reject("resetError", "Không thể cập nhật mật khẩu: " + e.getMessage());
            model.addAttribute("email", email);
            return "auth/verify-forgot-otp";
        }
    }

    @PostMapping("/forgot-password/resend-otp")
    public String resendForgotPasswordOtp(@RequestParam("email") String email,
                                          RedirectAttributes redirectAttributes) {
        if (accountRepository.existsByEmail(email)) {
            String newOtp = otpService.generateAndSaveForgotPasswordOtp(email);
            emailService.sendForgotPasswordOtpEmail(email, newOtp);
            redirectAttributes.addFlashAttribute("infoMessage", "Mã OTP mới đã được gửi lại thành công tới " + email);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Email không tồn tại trong hệ thống.");
            return "redirect:/forgot-password";
        }
        return "redirect:/forgot-password/verify-otp?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    // ==========================================
    // 4. TRANG XÁC NHẬN SAO CHÉP OTP TỪ EMAIL
    // ==========================================
    @GetMapping("/otp-action")
    public String showOtpActionModal(@RequestParam(value = "email", required = false) String email,
                                     @RequestParam(value = "otp", required = false) String otp,
                                     @RequestParam(value = "type", required = false, defaultValue = "register") String type,
                                     Model model) {
        String cleanEmail = (email != null) ? email.trim() : "";
        String cleanOtp = (otp != null) ? otp.trim() : "";
        String cleanType = (type != null) ? type.trim().toLowerCase() : "register";

        model.addAttribute("email", cleanEmail);
        model.addAttribute("otp", cleanOtp);
        model.addAttribute("type", cleanType);

        String targetVerifyUrl;
        if ("forgot".equals(cleanType) || "forgot-password".equals(cleanType)) {
            targetVerifyUrl = "/forgot-password/verify-otp?email=" + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8) + "&otp=" + cleanOtp;
        } else {
            targetVerifyUrl = "/register/verify-otp?email=" + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8) + "&otp=" + cleanOtp;
        }
        model.addAttribute("targetVerifyUrl", targetVerifyUrl);

        return "auth/otp-action";
    }
}