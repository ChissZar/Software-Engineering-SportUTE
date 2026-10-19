package vn.edu.ute.sportute.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserDTO {
    private String customerId;       // e.g. "KH001" (Khóa chính định danh duy nhất của khách hàng)
    private String accountId;        // e.g. "TK004" (Khóa chính tài khoản)
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String roleName;         // e.g. "Quản trị viên", "Quản lý trung tâm", "Lễ tân", "Kỹ thuật & Vận hành", "Khách hàng"
    private String primaryRole;      // e.g. "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_RECEPTIONIST", "ROLE_OPERATOR", "ROLE_CUSTOMER"
    private String avatarInitials;   // e.g. "QB", "TH", "MT", "QL"
    private String customAvatarUrl;  // e.g. "/customer/api/avatar/KH001"
    private Set<String> roles;       // e.g. ["ROLE_ADMIN", ...]

    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    public boolean isManager() {
        return roles != null && roles.contains("ROLE_MANAGER");
    }

    public boolean isReceptionist() {
        return roles != null && roles.contains("ROLE_RECEPTIONIST");
    }

    public boolean isOperator() {
        return roles != null && roles.contains("ROLE_OPERATOR");
    }

    public boolean isCustomer() {
        return roles != null && roles.contains("ROLE_CUSTOMER");
    }

    public boolean isHorizontalNav() {
        return (isReceptionist() || isOperator()) && !isAdmin() && !isManager();
    }

    // Role-based UI visibility helpers
    public boolean isCanManageSystem() {
        return isAdmin();
    }

    public boolean isCanManageCenter() {
        return isAdmin() || isManager();
    }

    public boolean isCanAccessBookings() {
        return isAdmin() || isManager() || isReceptionist() || isOperator();
    }

    public boolean isCanManagePayments() {
        return isAdmin() || isManager() || isReceptionist();
    }

    public boolean isCanManageMaintenance() {
        return isAdmin() || isManager() || isOperator();
    }

    public boolean isCanManageCustomers() {
        return isAdmin() || isManager() || isReceptionist();
    }
}
