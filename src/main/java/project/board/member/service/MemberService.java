package project.board.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.dto.request.MemberCreateRequest;
import project.board.member.dto.request.MemberUpdateRequest;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.repository.PostRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

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

    public Long count() {
        return memberRepository.count();
    }

    //todo: 개선
    public List<Member> findAllForAdmin() {
        return memberRepository.findAll();
    }

    @Transactional
    public void roleChange(String role, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new
                CustomException(ErrorCode.MEMBER_NOT_FOUND));

        member.changeRole(Role.valueOf(role));
    }

    @Transactional
    public void deleteForAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 회원이 작성한 댓글
        commentRepository.deleteAllByMember(member);

        // 회원이 작성한 게시글 안 댓글
        commentRepository.deleteAllByPostMember(member);

        // 회원이 작성한 게시글
        postRepository.deleteAllByMember(member);

        // 회원 제거
        memberRepository.delete(member);
    }

    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        memberRepository.delete(member);
    }


    public MemberUpdateRequest getMyProfile(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new
                CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberUpdateRequest.builder()
                .email(member.getEmail())
                .nickname(member.getNickname())
                .build();
    }

    @Transactional
    public void updateMyProfile(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new
                CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (StringUtils.hasText(request.getNickname())) {
            member.changeNickname(request.getNickname().trim());
        }

        if (StringUtils.hasText(request.getNickname())) {
            String newNickName = request.getNickname().trim();

            if (!newNickName.equals(member.getNickname()) && memberRepository.existsByNicknameAndIdNot(newNickName, memberId)) {
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
            }
            member.changeNickname(newNickName);
        }

        if (StringUtils.hasText(request.getNewPassword())) {

            // 현재 비밀번호 입력하지 않은 경우
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new CustomException(ErrorCode.PASSWORD_CURRENT_REQUIRED);
            }

            // 현재 비밀번호와 DB 비밀번호 다른 경우
            if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
                throw new CustomException(ErrorCode.PASSWORD_INVALID);
            }

            // 새로운 비밀번호 일치 여부
            if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
                throw new CustomException(ErrorCode.PASSWORD_CONFIRM);
            }

            String newPassword = request.getNewPassword();

            String encoded = passwordEncoder.encode(newPassword);

            member.changePassword(encoded);
        }
    }
}
