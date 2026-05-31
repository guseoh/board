package project.board.testsupport;

import org.springframework.test.util.ReflectionTestUtils;
import project.board.comment.entity.Comment;
import project.board.global.security.user.UnifiedPrincipal;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.post.entity.Post;

import java.time.LocalDateTime;
import java.util.Map;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Member member(Long id) {
        return member(id, "user" + id, "user" + id + "@example.com", Role.USER);
    }

    public static Member member(Long id, String nickname, String email, Role role) {
        Member member = Member.create(nickname, email, "encoded-password", role, LoginType.LOCAL);
        setId(member, id);
        return member;
    }

    public static Member oauthMember(Long id, String provider, String providerId) {
        Member member = Member.createOAuth(
                provider + "-user" + id,
                provider + id + "@example.com",
                "encoded-dummy",
                Role.USER,
                provider,
                providerId,
                LoginType.SOCIAL
        );
        setId(member, id);
        return member;
    }

    public static Post post(Long id, Member member) {
        return post(id, "title" + id, "content" + id, member);
    }

    public static Post post(Long id, String title, String content, Member member) {
        Post post = Post.create(title, content, member);
        setId(post, id);
        setAudit(post, LocalDateTime.now());
        return post;
    }

    public static Comment comment(Long id, String content, Member member, Post post) {
        Comment comment = Comment.create(content, member, post, null);
        setId(comment, id);
        setAudit(comment, LocalDateTime.now());
        return comment;
    }

    public static Comment reply(Long id, String content, Member member, Post post, Comment parent) {
        Comment comment = Comment.create(content, member, post, parent);
        setId(comment, id);
        setAudit(comment, LocalDateTime.now());
        return comment;
    }

    public static UnifiedPrincipal principal(Long memberId, Role role) {
        return UnifiedPrincipal.builder()
                .nickname("user" + memberId)
                .memberId(memberId)
                .email("user" + memberId + "@example.com")
                .role(role.getKey())
                .password("encoded-password")
                .loginType(LoginType.LOCAL)
                .attributes(Map.of())
                .build();
    }

    public static void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }

    public static void setAudit(Object target, LocalDateTime dateTime) {
        ReflectionTestUtils.setField(target, "createdAt", dateTime);
        ReflectionTestUtils.setField(target, "updatedAt", dateTime);
        ReflectionTestUtils.setField(target, "createdBy", "tester");
        ReflectionTestUtils.setField(target, "updatedBy", "tester");
    }
}
