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
import static org.mockito.ArgumentMatchers.anyString;
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
    @DisplayName("중복과 비밀번호 확인 검증 후 로컬 회원을 가입시킨다")
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
    @DisplayName("중복 이메일, 중복 닉네임, 비밀번호 불일치 시 회원가입 검증에 실패한다")
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
    @DisplayName("회원 수, 관리자 목록, 로컬 프로필을 조회한다")
    void readQueries() {
        Member member = member(1L);
        given(memberRepository.count()).willReturn(1L);
        given(memberRepository.findAll()).willReturn(List.of(member));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberUpdateResponse profile = memberService.getMyProfile(1L);

        assertThat(memberService.countMember()).isEqualTo(1L);
        assertThat(memberService.getMembersForAdmin()).containsExactly(member);
        assertThat(profile.getNickname()).isEqualTo(member.getNickname());
        assertThat(profile.isPasswordChangeable()).isTrue();
    }

    @Test
    @DisplayName("소셜 로그인 프로필은 비밀번호를 변경할 수 없도록 표시한다")
    void oauthProfileCannotChangePassword() {
        Member oauth = oauthMember(2L, "google", "google-1");
        given(memberRepository.findById(2L)).willReturn(Optional.of(oauth));

        MemberUpdateResponse profile = memberService.getMyProfile(2L);

        assertThat(profile.isPasswordChangeable()).isFalse();
    }

    @Test
    @DisplayName("닉네임을 공백 제거 후 수정하고 중복 닉네임은 거부한다")
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
    @DisplayName("비밀번호를 수정하고 현재 비밀번호와 확인 값을 검증한다")
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
    @DisplayName("역할을 변경하고 존재하지 않는 회원은 거부한다")
    void roleChange() {
        Member member = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        memberService.changeMemberRole(Role.ADMIN, 1L);

        assertThat(member.getRole()).isEqualTo(Role.ADMIN);
        assertThatThrownBy(() -> memberService.changeMemberRole(Role.ADMIN, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원 탈퇴와 관리자 삭제 시 연관 댓글과 게시글을 먼저 삭제한다")
    void deletePolicies() {
        Member member = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.withdraw(1L, "회원탈퇴");
        memberService.deleteMemberByAdmin(1L);

        verify(commentRepository, times(2)).deleteAllByMemberId(1L);
        verify(commentRepository, times(2)).deleteAllByPostMemberId(1L);
        verify(postRepository, times(2)).deleteAllByMemberId(1L);
        verify(memberRepository, times(2)).deleteById(1L);
    }

    @Test
    @DisplayName("회원 탈퇴 확인 문구가 다르면 어떤 삭제도 수행하지 않는다")
    void withdrawRejectsInvalidConfirmation() {
        Member member = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.withdraw(1L, "회원 탈퇴"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.WITHDRAW_CONFIRMATION_MISMATCH.getMessage());
        assertThatThrownBy(() -> memberService.withdraw(1L, ""))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.WITHDRAW_CONFIRMATION_MISMATCH.getMessage());

        verify(commentRepository, never()).deleteAllByMemberId(1L);
        verify(commentRepository, never()).deleteAllByPostMemberId(1L);
        verify(postRepository, never()).deleteAllByMemberId(1L);
        verify(memberRepository, never()).deleteById(1L);
    }

    @Test
    @DisplayName("프로필 대상 회원이 없으면 예외를 던진다")
    void missingMember() {
        given(memberRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyProfile(404L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("소셜 회원은 비밀번호를 변경할 수 없다")
    void socialMemberCannotUpdatePassword() {
        Member socialMember = oauthMember(2L, "google", "google-1");
        String originalPassword = socialMember.getPassword();

        MemberPasswordUpdateRequest request =
                passwordRequest("current1", "newpass1", "newpass1");

        given(memberRepository.findById(2L))
                .willReturn(Optional.of(socialMember));

        assertThatThrownBy(() -> memberService.updatePassword(2L, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SOCIAL_PASSWORD_CHANGE_NOT_ALLOWED.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());

        assertThat(socialMember.getPassword()).isEqualTo(originalPassword);
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
