package project.board.member.dto.response;

import lombok.Builder;
import lombok.Getter;
import project.board.member.entity.Member;
import project.board.member.entity.Role;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberResponse {

    private Long id;
    private String nickname;
    private String email;
    private Role role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .createdBy(member.getCreatedBy())
                .updatedBy(member.getUpdatedBy())
                .build();
    }

}
