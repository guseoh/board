package project.board.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.dto.request.MemberCreateRequest;
import project.board.member.dto.request.MemberUpdateRequest;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.repository.PostRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    @DisplayName("회원가입은 이메일과 닉네임이 중복되지 않고 비밀번호 확인이 일치하면 USER 권한 회원을 저장한다")
    void signUp_success_savesUserWithEncodedPassword() {
        // given
        MemberCreateRequest request = createRequest("tester@example.com", "password1", "password1");
        given(memberRepository.existsByEmail("tester@example.com")).willReturn(false);
        given(memberRepository.existsByNickname("테스터")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("encoded-password");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // when
        Long memberId = memberService.signUp(request);

        // then
        assertThat(memberId).isEqualTo(1L);
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getNickname()).isEqualTo("테스터");
        assertThat(savedMember.getEmail()).isEqualTo("tester@example.com");
        assertThat(savedMember.getPassword()).isEqualTo("encoded-password");
        assertThat(savedMember.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("회원가입은 이미 사용 중인 이메일이면 예외를 던지고 저장하지 않는다")
    void signUp_duplicateEmail_throwsException() {
        // given
        MemberCreateRequest request = createRequest("duplicate@example.com", "password1", "password1");
        given(memberRepository.existsByEmail("duplicate@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.signUp(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입은 비밀번호와 비밀번호 확인이 다르면 예외를 던지고 저장하지 않는다")
    void signUp_passwordMismatch_throwsException() {
        // given
        MemberCreateRequest request = createRequest("tester@example.com", "password1", "different1");
        given(memberRepository.existsByEmail("tester@example.com")).willReturn(false);
        given(memberRepository.existsByNickname("테스터")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.signUp(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PASSWORD_MISMATCH.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("프로필 수정은 닉네임 공백을 제거하고 현재 비밀번호 검증 후 새 비밀번호로 변경한다")
    void updateMyProfile_success_trimsNicknameAndChangesPassword() {
        // given
        Long memberId = 1L;
        Member member = member(memberId, "기존닉네임", "member@example.com", "encoded-current", Role.USER);
        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .nickname("  새닉네임  ")
                .currentPassword("current1")
                .newPassword("newpass1")
                .newPasswordConfirm("newpass1")
                .build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("current1", "encoded-current")).willReturn(true);
        given(passwordEncoder.encode("newpass1")).willReturn("encoded-new");

        // when
        memberService.updateMyProfile(memberId, request);

        // then
        assertThat(member.getNickname()).isEqualTo("새닉네임");
        assertThat(member.getPassword()).isEqualTo("encoded-new");
    }

    @Test
    @DisplayName("프로필 수정은 새 비밀번호 입력 시 현재 비밀번호가 틀리면 예외를 던진다")
    void updateMyProfile_invalidCurrentPassword_throwsException() {
        // given
        Long memberId = 1L;
        Member member = member(memberId, "기존닉네임", "member@example.com", "encoded-current", Role.USER);
        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .currentPassword("wrongpass1")
                .newPassword("newpass1")
                .newPasswordConfirm("newpass1")
                .build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongpass1", "encoded-current")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.updateMyProfile(memberId, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PASSWORD_INVALID.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("관리자 회원 삭제는 회원의 댓글, 회원 게시글의 댓글, 회원 게시글을 먼저 삭제한 뒤 회원을 삭제한다")
    void deleteForAdmin_success_deletesRelatedDataBeforeMember() {
        // given
        Long memberId = 1L;
        Member member = member(memberId, "삭제회원", "delete@example.com", "encoded", Role.USER);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        memberService.deleteForAdmin(memberId);

        // then
        verify(commentRepository).deleteAllByMemberId(memberId);
        verify(commentRepository).deleteAllByPostMemberId(memberId);
        verify(postRepository).deleteAllByMemberId(memberId);
        verify(memberRepository).delete(member);
    }

    private MemberCreateRequest createRequest(String email, String password, String passwordConfirm) {
        MemberCreateRequest request = new MemberCreateRequest();
        request.setNickname("테스터");
        request.setEmail(email);
        request.setPassword(password);
        request.setPasswordConfirm(passwordConfirm);
        return request;
    }

    private Member member(Long id, String nickname, String email, String password, Role role) {
        Member member = Member.create(nickname, email, password, role);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
