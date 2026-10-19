package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.response.ChatbotMessageResponse;

public interface SportBotService {
    /**
     * Xử lý câu hỏi của người dùng và truy vấn dữ liệu thời gian thực từ cơ sở dữ liệu.
     * @param userMessage Câu hỏi của khách hàng
     * @param usernameOrEmail Tài khoản khách hàng nếu đã đăng nhập (hoặc null nếu khách vãng lai)
     * @return Phản hồi được định dạng đẹp mắt kèm dữ liệu thời gian thực và các gợi ý tiếp theo
     */
    ChatbotMessageResponse handleMessage(String userMessage, String usernameOrEmail);
}
