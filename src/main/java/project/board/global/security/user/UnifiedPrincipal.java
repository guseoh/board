package project.board.global.security.user;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import project.board.member.entity.Member;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class UnifiedPrincipal implements UserDetails, OAuth2User {

    // 공통
    private final String nickname;
    private final Long memberId;
    private final String email;
    private final String role;

    // formLogin
    private final String password;

    // oauth
    private final String provider;
    private final String providerId;
    private final Map<String, Object> attributes;

    public UnifiedPrincipal(String nickname, Long memberId, String email, String role, String password, String provider, String providerId, Map<String, Object> attributes) {
        this.nickname = nickname;
        this.memberId = memberId;
        this.email = email;
        this.role = role;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        this.attributes = attributes;
    }

    public static UnifiedPrincipal from(Member member) {
        return new UnifiedPrincipal(
                member.getNickname(),
                member.getId(),
                member.getEmail(),
                member.getRole().getKey(),
                member.getPassword(),
                null,
                null,
                null
        );
    }

    @Override
    public String getName() {
        return email;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }


    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }


}
