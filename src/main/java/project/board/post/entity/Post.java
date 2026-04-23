package project.board.post.entity;

import jakarta.persistence.*;
import lombok.*;
import project.board.comment.entity.Comment;
import project.board.global.entity.BaseEntity;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.Member;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    private int viewCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "post")
    private List<Comment> comments = new ArrayList<>();

    public static Post create(String title, String content, Member member) {

        validateTitle(title);

        validateContent(content);

        validateMember(member);

        Post p = new Post();
        p.title = title;
        p.content = content;
        p.assignMember(member);
        return p;
    }

    private static void validateMember(Member member) {
        if (member == null) {
            throw new CustomException(ErrorCode.POST_NOT_MEMBER);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CustomException(ErrorCode.POST_NOT_CONTENT);
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new CustomException(ErrorCode.POST_NOT_TITLE);
        }
    }

    public void change(String title, String content) {
        validateTitle(title);
        validateContent(content);
        this.title = title;
        this.content = content;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.addPost(this);
    }

    // 작성자 변경이 불가능하다.
    private void assignMember(Member member) {

        // 회원
        if (member == null) {
            throw new CustomException(ErrorCode.POST_NOT_MEMBER);
        }

        // 작성자
        if (this.member != null) {
            throw new CustomException(ErrorCode.POST_WRITER_CANNOT_CHANGE);
        }

        this.member = member;

        if (!member.getPosts().contains(this)) {
            member.getPosts().add(this);
        }
    }
}
