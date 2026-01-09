package project.board.member.entity;

import jakarta.persistence.*;
import lombok.*;
import project.board.comment.entity.Comment;
import project.board.global.entity.BaseEntity;
import project.board.post.entity.Post;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
//@AllArgsConstructor : 정적 팩토리
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)       // 보안상 null 방지
    private Role role;

    @OneToMany(mappedBy = "member")
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Comment> comments = new ArrayList<>();

    public static Member create(String nickname, String email, String encodedPassword, Role role) {
        Member m = new Member();
        m.nickname = nickname;
        m.email = email;
        m.password = encodedPassword;
        m.role = role;
        return m;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String password) {
        this.password = password;
    }
}

/*
    정적 팩토리 메서드 vs 빌더 패턴
 */