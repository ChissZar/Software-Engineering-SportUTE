package vn.edu.ute.sportute.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.sportute.dto.request.ChatbotMessageRequest;
import vn.edu.ute.sportute.dto.response.ChatbotMessageResponse;
import vn.edu.ute.sportute.service.SportBotService;

@RestController
public class CustomerChatbotController {

    private final SportBotService sportBotService;

    public CustomerChatbotController(SportBotService sportBotService) {
        this.sportBotService = sportBotService;
    }

    @PostMapping({"/customer/api/chat", "/api/chat"})
    public ResponseEntity<ChatbotMessageResponse> chat(@RequestBody ChatbotMessageRequest request,
                                                      Authentication authentication) {
        String usernameOrEmail = (authentication != null) ? authentication.getName() : null;
        String message = (request != null) ? request.getMessage() : "";
        ChatbotMessageResponse response = sportBotService.handleMessage(message, usernameOrEmail);
        return ResponseEntity.ok(response);
    }
}
