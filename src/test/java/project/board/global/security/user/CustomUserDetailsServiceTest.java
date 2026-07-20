package project.board.global.security.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.board.global.security.principal.UnifiedPrincipal;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static project.board.testsupport.TestFixtures.member;
import static project.board.testsupport.TestFixtures.oauthMember;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("form login은 LOCAL 회원만 UnifiedPrincipal로 조회한다")
    void loadsOnlyLocalMember() {
        Member local = member(1L);
        given(memberRepository.findByEmailAndLoginType(local.getEmail(), LoginType.LOCAL))
                .willReturn(Optional.of(local));
        given(passwordEncoder.matches("password1", local.getPassword())).willReturn(true);

        UnifiedPrincipal principal = (UnifiedPrincipal) provider().authenticate(
                new UsernamePasswordAuthenticationToken(local.getEmail(), "password1")
        ).getPrincipal();

        assertThat(principal.getMemberId()).isEqualTo(local.getId());
        assertThat(principal.getLoginType()).isEqualTo(LoginType.LOCAL);
    }

    @Test
    @DisplayName("SOCIAL 회원과 존재하지 않는 회원은 form login에서 같은 오류로 거부한다")
    void rejectsSocialAndMissingMemberWithoutTypeDisclosure() {
        Member social = oauthMember(2L, "google", "google-1");
        given(memberRepository.findByEmailAndLoginType(social.getEmail(), LoginType.LOCAL))
                .willReturn(Optional.empty());
        given(memberRepository.findByEmailAndLoginType("missing@example.com", LoginType.LOCAL))
                .willReturn(Optional.empty());
        given(passwordEncoder.encode(org.mockito.ArgumentMatchers.any())).willReturn("encoded-not-found");

        Throwable socialFailure = catchThrowable(
                () -> provider().authenticate(new UsernamePasswordAuthenticationToken(social.getEmail(), "dummy"))
        );
        Throwable missingFailure = catchThrowable(
                () -> provider().authenticate(new UsernamePasswordAuthenticationToken("missing@example.com", "dummy"))
        );

        assertThat(socialFailure).isInstanceOf(BadCredentialsException.class);
        assertThat(missingFailure).isInstanceOf(BadCredentialsException.class);
        assertThat(socialFailure).hasMessage(missingFailure.getMessage());

    }

    private DaoAuthenticationProvider provider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(new CustomUserDetailsService(memberRepository));
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
