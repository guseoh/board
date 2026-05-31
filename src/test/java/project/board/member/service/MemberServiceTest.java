package project.board.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.dto.request.MemberCreateRequest;
import project.board.member.dto.request.MemberNicknameUpdateRequest;
import project.board.member.dto.request.MemberPasswordUpdateRequest;
import project.board.member.dto.response.MemberUpdateResponse;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.repository.PostRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static project.board.testsupport.TestFixtures.member;
import static project.board.testsupport.TestFixtures.oauthMember;
import static project.board.testsupport.TestFixtures.setId;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("signs up a local user after duplicate and password checks")
    void signUpSuccess() {
        MemberCreateRequest request = createRequest("tester@example.com", "tester", "password1", "password1");
        given(memberRepository.existsByEmail("tester@example.com")).willReturn(false);
        given(memberRepository.existsByNickname("tester")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("encoded");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member saved = invocation.getArgument(0);
            setId(saved, 1L);
            return saved;
        });

        Long memberId = memberService.signUp(request);

        assertThat(memberId).isEqualTo(1L);
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(captor.getValue().getLoginType()).isEqualTo(LoginType.LOCAL);
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("rejects duplicate email, duplicate nickname and password mismatch")
    void signUpValidationFailures() {
        MemberCreateRequest duplicateEmail = createRequest("dup@example.com", "tester", "password1", "password1");
        given(memberRepository.existsByEmail("dup@example.com")).willReturn(true);
        assertThatThrownBy(() -> memberService.signUp(duplicateEmail))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());

        MemberCreateRequest duplicateNickname = createRequest("ok@example.com", "dup", "password1", "password1");
        given(memberRepository.existsByEmail("ok@example.com")).willReturn(false);
        given(memberRepository.existsByNickname("dup")).willReturn(true);
        assertThatThrownBy(() -> memberService.signUp(duplicateNickname))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.DUPLICATE_NICKNAME.getMessage());

        MemberCreateRequest mismatch = createRequest("ok2@example.com", "ok", "password1", "password2");
        given(memberRepository.existsByEmail("ok2@example.com")).willReturn(false);
        given(memberRepository.existsByNickname("ok")).willReturn(false);
        assertThatThrownBy(() -> memberService.signUp(mismatch))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PASSWORD_MISMATCH.getMessage());

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("reads member counts, admin list and local profile")
    void readQueries() {
        Member member = member(1L);
        given(memberRepository.count()).willReturn(1L);
        given(memberRepository.findAll()).willReturn(List.of(member));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberUpdateResponse profile = memberService.getMyProfile(1L);

        assertThat(memberService.countMember()).isEqualTo(1L);
        assertThat(memberService.findAllForAdmin()).containsExactly(member);
        assertThat(profile.getNickname()).isEqualTo(member.getNickname());
        assertThat(profile.isPasswordChangeable()).isTrue();
    }

    @Test
    @DisplayName("marks OAuth profile as not password changeable")
    void oauthProfileCannotChangePassword() {
        Member oauth = oauthMember(2L, "google", "google-1");
        given(memberRepository.findById(2L)).willReturn(Optional.of(oauth));

        MemberUpdateResponse profile = memberService.getMyProfile(2L);

        assertThat(profile.isPasswordChangeable()).isFalse();
    }

    @Test
    @DisplayName("updates nickname after trimming and rejects duplicate nickname")
    void updateNickname() {
        Member member = member(1L, "old", "old@example.com", Role.USER);
        MemberNicknameUpdateRequest request = nicknameRequest(" new ");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNicknameAndIdNot("new", 1L)).willReturn(false);

        MemberUpdateResponse response = memberService.updateNickname(1L, request);

        assertThat(response.getNickname()).isEqualTo("new");
        assertThat(member.getNickname()).isEqualTo("new");

        MemberNicknameUpdateRequest duplicate = nicknameRequest("dup");
        given(memberRepository.existsByNicknameAndIdNot("dup", 1L)).willReturn(true);
        assertThatThrownBy(() -> memberService.updateNickname(1L, duplicate))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("updates password and validates current and confirmation values")
    void updatePassword() {
        Member member = member(1L, "user", "user@example.com", Role.USER);
        MemberPasswordUpdateRequest request = passwordRequest("current1", "newpass1", "newpass1");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("current1", member.getPassword())).willReturn(true);
        given(passwordEncoder.encode("newpass1")).willReturn("encoded-new");

        memberService.updatePassword(1L, request);

        assertThat(member.getPassword()).isEqualTo("encoded-new");

        assertThatThrownBy(() -> memberService.updatePassword(1L, passwordRequest("", "newpass1", "newpass1")))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PASSWORD_CURRENT_REQUIRED.getMessage());

        given(passwordEncoder.matches("wrongpass1", "encoded-new")).willReturn(false);
        assertThatThrownBy(() -> memberService.updatePassword(1L, passwordRequest("wrongpass1", "newpass1", "newpass1")))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PASSWORD_INVALID.getMessage());

        given(passwordEncoder.matches("current1", "encoded-new")).willReturn(true);
        assertThatThrownBy(() -> memberService.updatePassword(1L, passwordRequest("current1", "newpass1", "different1")))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PASSWORD_CONFIRM.getMessage());
    }

    @Test
    @DisplayName("changes role and rejects missing member")
    void roleChange() {
        Member member = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        memberService.roleChange("ADMIN", 1L);

        assertThat(member.getRole()).isEqualTo(Role.ADMIN);
        assertThatThrownBy(() -> memberService.roleChange("ADMIN", 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("withdraw and admin delete remove dependent comments and posts first")
    void deletePolicies() {
        Member member = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.withdraw(1L);
        memberService.deleteForAdmin(1L);

        verify(commentRepository, times(2)).deleteAllByMemberId(1L);
        verify(commentRepository, times(2)).deleteAllByPostMemberId(1L);
        verify(postRepository, times(2)).deleteAllByMemberId(1L);
        verify(memberRepository, times(2)).deleteById(1L);
    }

    @Test
    @DisplayName("throws when profile target member is missing")
    void missingMember() {
        given(memberRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyProfile(404L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    private MemberCreateRequest createRequest(String email, String nickname, String password, String confirm) {
        MemberCreateRequest request = new MemberCreateRequest();
        request.setEmail(email);
        request.setNickname(nickname);
        request.setPassword(password);
        request.setPasswordConfirm(confirm);
        return request;
    }

    private MemberNicknameUpdateRequest nicknameRequest(String nickname) {
        MemberNicknameUpdateRequest request = new MemberNicknameUpdateRequest();
        request.setNickname(nickname);
        return request;
    }

    private MemberPasswordUpdateRequest passwordRequest(String current, String password, String confirm) {
        MemberPasswordUpdateRequest request = new MemberPasswordUpdateRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(password);
        request.setNewPasswordConfirm(confirm);
        return request;
    }
}
