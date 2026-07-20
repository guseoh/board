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
import project.board.member.dto.request.MemberNicknameUpdateRequest;
import project.board.member.dto.request.MemberPasswordUpdateRequest;
import project.board.member.dto.response.MemberUpdateResponse;
import project.board.member.entity.LoginType;
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
                Role.USER,
                LoginType.LOCAL
        );

        return memberRepository.save(member).getId();
    }

    public Long countMember() {
        return memberRepository.count();
    }

    public List<Member> getMembersForAdmin() {
        return memberRepository.findAll();
    }

    @Transactional
    public void changeMemberRole(String role, Long memberId) {
        Member member = validateMember(memberId);

        member.changeRole(Role.valueOf(role));
    }

    @Transactional
    public void deleteMemberByAdmin(Long memberId) {
        validateMember(memberId);

        MemberRemovalPolicy(memberId);
    }

    @Transactional
    public void withdraw(Long memberId) {
        validateMember(memberId);

        // 회원이 작성한 댓글
        MemberRemovalPolicy(memberId);
    }


    public MemberUpdateResponse getMyProfile(Long memberId) {
        Member member = validateMember(memberId);

        return MemberUpdateResponse.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .passwordChangeable(member.getLoginType() == LoginType.LOCAL)
                .build();
    }

    @Transactional
    public void updatePassword(Long memberId, MemberPasswordUpdateRequest request) {

        Member member = validateMember(memberId);

        // 소셜회원은 비밀번호 변경 차단
        if (member.getLoginType() != LoginType.LOCAL) {
            throw new CustomException(ErrorCode.SOCIAL_PASSWORD_CHANGE_NOT_ALLOWED);
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

    @Transactional
    public MemberUpdateResponse updateNickname(Long memberId, MemberNicknameUpdateRequest request) {

        Member member = validateMember(memberId);

        if (StringUtils.hasText(request.getNickname())) {
            String newNickName = request.getNickname().trim();

            if (!newNickName.equals(member.getNickname()) && memberRepository.existsByNicknameAndIdNot(newNickName, memberId)) {
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
            }
            member.changeNickname(newNickName);
        }

        return MemberUpdateResponse.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .build();
    }

    private Member validateMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void MemberRemovalPolicy(Long memberId) {
        // 회원이 작성한 댓글
        commentRepository.deleteAllByMemberId(memberId);

        // 회원이 작성한 게시글에 달린 댓글
        commentRepository.deleteAllByPostMemberId(memberId);

        // 회원이 작성한 게시글
        postRepository.deleteAllByMemberId(memberId);

        // 회원 제거
        memberRepository.deleteById(memberId);
    }
}
