package project.board.member.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberUpdateResponse {

    private String nickname;

    private String email;

    // 소셜 로그인 구분용
    private boolean passwordChangeable;
}
