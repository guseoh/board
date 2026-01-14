package project.board.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import project.board.member.dto.MemberDto;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signUp(MemberDto.CreateRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        String encoded = passwordEncoder.encode(request.getPassword());

        Member member = Member.create(
                request.getNickname(),
                request.getEmail(),
                encoded,
                Role.USER
        );

        return memberRepository.save(member).getId();
    }

    public MemberDto.Response getMyProfile(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new
                IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        return MemberDto.Response.from(member);
    }

    @Transactional
    public void updateMyProfile(Long memberId, MemberDto.UpdateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new
                IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        //이메일 변경
        if (StringUtils.hasText(request.getEmail())) {
            String newEmail = request.getEmail().trim();

            if (!newEmail.equals(member.getEmail())
                    && memberRepository.existsByEmailAndIdNot(newEmail, memberId)
            ) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }

            member.changeEmail(newEmail);
        }

        //닉네임 변경
        if (StringUtils.hasText(request.getNickname())) {
            String newNickName = request.getNickname().trim();

            if (!newNickName.equals(request.getNickname()) && memberRepository.existsByNicknameAndIdNot(newNickName, memberId)) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            member.changeNickname(newNickName);
        }

        //비밀번호 변경
        if (StringUtils.hasText(request.getPassword())) {
            String rawPw = request.getPassword();

            String encoded = passwordEncoder.encode(rawPw);
            member.changePassword(encoded);
        }
    }

}
