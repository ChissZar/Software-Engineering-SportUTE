package vn.edu.ute.sportute.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.ute.sportute.entity.Account;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Account account;
    private final String username;
    private final String password;
    private final String status;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(Account account) {
        this.account = account;
        this.username = (account != null && account.getUsername() != null) ? account.getUsername() : "";
        this.password = (account != null && account.getPassword() != null) ? account.getPassword() : "";
        this.status = (account != null && account.getStatus() != null) ? account.getStatus() : "Hoạt động";
        if (account != null && account.getRoles() != null) {
            this.authorities = account.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList());
        } else {
            this.authorities = List.of();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return !"Khóa".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return "Hoạt động".equalsIgnoreCase(status);
    }
}