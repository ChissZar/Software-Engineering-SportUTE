package vn.edu.ute.sportute.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageResponse {
    private String reply;
    private String status;               // "SUCCESS", "ERROR"
    private List<String> suggestions;    // Gợi ý câu hỏi tiếp theo
    private Map<String, Object> actionData; // Dữ liệu bổ trợ để frontend tự động chọn sân hoặc điều hướng
}
