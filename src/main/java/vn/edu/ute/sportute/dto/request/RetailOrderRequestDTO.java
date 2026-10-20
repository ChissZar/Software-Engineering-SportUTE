package vn.edu.ute.sportute.dto.request;

import java.util.List;

public class RetailOrderRequestDTO {
    // 1. Khách hàng mua lẻ (Có thể để trống để tự động gán là Khách vãng lai)
    private String customerName;
    private String customerPhone;

    // 2. Hình thức thanh toán: "Tiền mặt", "VietQR", "Chuyển khoản"
    private String paymentMethod;

    // 3. Ghi chú đơn bán lẻ
    private String note;

    // 4. Danh sách các món F&B / Dụng cụ được mua
    private List<AddServiceOrderRequestDTO> items;

    public RetailOrderRequestDTO() {}

    public RetailOrderRequestDTO(String customerName, String customerPhone, String paymentMethod, String note, List<AddServiceOrderRequestDTO> items) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.paymentMethod = paymentMethod;
        this.note = note;
        this.items = items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<AddServiceOrderRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<AddServiceOrderRequestDTO> items) {
        this.items = items;
    }
}
