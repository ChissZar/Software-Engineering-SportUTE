package vn.edu.ute.sportute.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountDTO {
    private String id;
    private String username;
    private String email;
    private String status;
    private String fullName;
    private String phone;
    private String accountType;
    private String avatarClass;
    private String avatarInitials;

    @Builder.Default
    private Set<String> roleIds = new HashSet<>();

    @Builder.Default
    private Set<String> roleNames = new HashSet<>();

    private String roleFilterKeys;
    private String searchData;
}
