package project.board.global.security.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 사용자 조회 + 비밀번호 검증 (User Dto)
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final String nickname;
    private final String password;
    private final Long memberId;
    private final String email;
    private final String role;

    //private final Member member;

    public CustomUserDetails(String nickname, String password, Long memberId, String email, String role) {
        this.nickname = nickname;
        this.password = password;
        this.memberId = memberId;
        this.email = email;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
