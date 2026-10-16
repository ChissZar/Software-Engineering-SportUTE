package vn.edu.ute.sportute.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý ngoại lệ toàn cục cho ứng dụng SportUTE (HTML UI & REST API).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException ex,
                                                 HttpServletRequest request,
                                                 RedirectAttributes redirectAttributes,
                                                 Model model) {
        log.warn("Validation / Business logic exception on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return resolveResponse(ex.getMessage(), request, redirectAttributes, model, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalStateException(IllegalStateException ex,
                                             HttpServletRequest request,
                                             RedirectAttributes redirectAttributes,
                                             Model model) {
        log.warn("Illegal state exception on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return resolveResponse(ex.getMessage(), request, redirectAttributes, model, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                              HttpServletRequest request,
                                              RedirectAttributes redirectAttributes,
                                              Model model) {
        String defaultMsg = ex.getBindingResult().getAllErrors().isEmpty()
                ? "Dữ liệu gửi lên không hợp lệ"
                : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Form binding validation error on [{}]: {}", request.getRequestURI(), defaultMsg);
        return resolveResponse(defaultMsg, request, redirectAttributes, model, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        log.error("Unhandled internal server error on [{}]: ", request.getRequestURI(), ex);
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Hệ thống gặp sự cố trong quá trình xử lý yêu cầu!";
        return resolveResponse(msg, request, redirectAttributes, model, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Object resolveResponse(String message,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes,
                                   Model model,
                                   HttpStatus status) {
        boolean isApi = isApiRequest(request);

        if (isApi) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("error", true);
            errorBody.put("message", message);
            errorBody.put("status", status.value());
            return ResponseEntity.status(status).body(errorBody);
        }

        // Với request HTML từ trình duyệt
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            redirectAttributes.addFlashAttribute("errorMessage", message);
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isBlank()) {
                return "redirect:" + referer;
            }
            if (request.getRequestURI().startsWith("/customer")) {
                return "redirect:/customer/dashboard";
            }
            return "redirect:/admin/dashboard";
        }

        // Với GET request thất bại, chuyển đến trang error đẹp mắt
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", message);
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/") || uri.contains("/api/")) {
            return true;
        }
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            return true;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }
}
