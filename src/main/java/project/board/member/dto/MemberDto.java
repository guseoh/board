//package project.board.member.dto;
//
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Pattern;
//import lombok.*;
//import project.board.member.entity.Member;
//import project.board.member.entity.Role;
//
//import java.time.LocalDateTime;
//
//public class MemberDto {
//
//    @Getter
//    @NoArgsConstructor
//    @Setter
//    public static class CreateRequest {
//
//        @Pattern(
//                regexp = "^[a-zA-Z0-9가-힣]{2,12}$",
//                message = "닉네임은 2~12자, 영문/숫자/한글만 가능합니다.")
//        @NotBlank(message = "닉네임은 필수입니다.")
//        private String nickname;
//
//        @Pattern(
//                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
//                message = "비밀번호는 8~20자, 영문과 숫자를 각각 1개 이상 포함해야 합니다."
//        )
//        @NotBlank(message = "비밀번호는 필수입니다.")
//        private String password;
//
//        @NotBlank(message = "비밀번호 확인은 필수입니다.")
//        private String passwordConfirm;
//
//        @Email(message = "이메일 형식이 올바르지 않습니다.")
//        @NotBlank(message = "이메일은 필수입니다.")
//        private String email;
//
//    }
//
//    @Getter
//    @NoArgsConstructor
//    @Setter
//    public static class UpdateRequest {
//
//        @Pattern(
//                regexp = "^[a-zA-Z0-9가-힣]{2,12}$",
//                message = "닉네임은 2~12자, 영문/숫자/한글만 가능합니다.")
//        private String nickname;
//
//        @Pattern(
//                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
//                message = "비밀번호는 8~20자, 영문과 숫자를 각각 1개 이상 포함해야 합니다."
//        )
//        private String password;
//
//        @Email(message = "이메일 형식이 올바르지 않습니다.")
//        private String email;
//    }
//
//    @Getter
//    @Builder
//    public static class Response {
//
//        private Long id;
//        private String nickname;
//        private String email;
//        private Role role;
//
//        private LocalDateTime createdAt;
//        private LocalDateTime updatedAt;
//        private String createdBy;
//        private String updatedBy;
//
//        public static Response from(Member member) {
//            return Response.builder()
//                    .id(member.getId())
//                    .nickname(member.getNickname())
//                    .email(member.getEmail())
//                    .role(member.getRole())
//                    .build();
//        }
//    }
//
//
//}
