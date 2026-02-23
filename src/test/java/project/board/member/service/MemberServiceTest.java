package project.board.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import project.board.global.exception.CustomException;
import project.board.member.dto.MemberDto;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    PasswordEncoder passwordEncoder;

    private MemberDto.CreateRequest createRequest(String nickname, String email, String Password) {
        MemberDto.CreateRequest req = new MemberDto.CreateRequest();
        req.setNickname(nickname);
        req.setEmail(email);
        req.setPassword(Password);

        return req;
    }

    private MemberDto.UpdateRequest updateReq(String nickname, String email, String pw) {
        MemberDto.UpdateRequest req = new MemberDto.UpdateRequest();
        req.setNickname(nickname);
        req.setEmail(email);
        req.setPassword(pw);
        return req;
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void 회원가입_성공() {

        //given
        MemberDto.CreateRequest req = createRequest("테스트", "test@te.com", "1234");

        //when
        Long id = memberService.signUp(req);

        //then
        Member member = memberRepository.findById(id).orElseThrow();
        assertThat(member.getNickname()).isEqualTo("테스트");
        assertThat(member.getEmail()).isEqualTo("test@te.com");
        assertThat(member.getRole()).isEqualTo(Role.USER);

        assertThat(member.getPassword()).isNotEqualTo("1234");
        assertThat(passwordEncoder.matches("1234", member.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 이메일 중복 테스트")
    void 회원가입_실패() {

        memberService.signUp(createRequest("테스트", "test@te.com", "1234"));

        assertThatThrownBy(() ->
                memberService.signUp(createRequest("실패", "test@te.com", "1234")))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 사용중인 이메일입니다.");
    }
    @Test
    @DisplayName("회원가입 실패 닉네임 중복 테스트")
    void 회원가입_실패2() {

        memberService.signUp(createRequest("테스트", "test@te.com", "1234"));

        assertThatThrownBy(() ->
                memberService.signUp(createRequest("테스트", "st@te.com", "1234")))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 사용중인 닉네임입니다.");
    }

    @Test
    @DisplayName("내 정보 조회 실패 테스트")
    void 정보조회() {
        assertThatThrownBy(() ->
                memberService.getMyProfile(9999L))
                .isInstanceOf(CustomException.class)
                .hasMessage("해당 사용자가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("프로필 수정 테스트")
    void 프로필수정() {

        //given
        Long id = memberService.signUp(createRequest("테스트", "test@te.com", "1234"));

        var req = updateReq(" 새로운이름 ", "test@te.com", "1234");

        //when
        memberService.updateMyProfile(id, req);

        //then
        Member member = memberRepository.findById(id).orElseThrow();
        assertThat(member.getNickname()).isEqualTo("새로운이름");
        assertThat(member.getEmail()).isEqualTo("test@te.com");
        assertThat(passwordEncoder.matches("1234", member.getPassword())).isTrue();
    }
}