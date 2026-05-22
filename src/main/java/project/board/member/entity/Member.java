package project.board.member.entity;

import jakarta.persistence.*;
import lombok.*;
import project.board.global.entity.BaseEntity;
import project.board.post.entity.Post;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    private String provider;    // ex) google

    private String providerId;  // ex) 구글 로그인 한 유저의 고유 ID가 들어감

    @Enumerated(EnumType.STRING)
    private LoginType loginType;    // 로그인 구분

    public static Member create(String nickname, String email, String encodedPassword, Role role, LoginType loginType) {
        Member m = new Member();
        m.nickname = nickname;
        m.email = email;
        m.password = encodedPassword;
        m.role = role;
        m.loginType = loginType;
        return m;
    }

    // OAuth Member 생성
    public static Member createOAuth(String nickname, String email, String DummyPassword, Role role, String provider, String providerId, LoginType loginType) {
        Member m = new Member();
        m.nickname = nickname;
        m.email = email;
        m.password = DummyPassword;
        m.role = role;
        m.provider = provider;
        m.providerId = providerId;
        m.loginType = loginType;
        return m;
    }


    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeRole(Role role) {
        this.role = role;
    }
}
