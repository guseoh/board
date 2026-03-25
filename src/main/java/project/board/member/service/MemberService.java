package project.board.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.dto.request.MemberCreateRequest;
import project.board.member.dto.request.MemberUpdateRequest;
import project.board.member.dto.response.MemberResponse;
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
    public Long signUp(MemberCreateRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
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

    public MemberResponse getMyProfile(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new
                CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponse.from(member);
    }

    @Transactional
    public void updateMyProfile(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new
                CustomException(ErrorCode.MEMBER_NOT_FOUND));

        //이메일 변경
        if (StringUtils.hasText(request.getEmail())) {
            String newEmail = request.getEmail().trim();

            if (!newEmail.equals(member.getEmail())
                    && memberRepository.existsByEmailAndIdNot(newEmail, memberId)
            ) {
                throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
            }

            member.changeEmail(newEmail);
        }

        //닉네임 변경
        if (StringUtils.hasText(request.getNickname())) {
            String newNickName = request.getNickname().trim();

            if (!newNickName.equals(request.getNickname()) && memberRepository.existsByNicknameAndIdNot(newNickName, memberId)) {
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
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
