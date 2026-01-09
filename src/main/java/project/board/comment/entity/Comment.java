package project.board.comment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.board.global.entity.BaseEntity;
import project.board.member.entity.Member;
import project.board.post.entity.Post;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public static Comment create(String content, Member member, Post post) {
        Comment c = new Comment();
        c.content = content;
        c.addMember(member);
        c.addPost(post);
        return c;
    }

    public void addMember(Member member) {
        this.member = member;
        member.getComments().add(this);
    }

    public void addPost(Post post) {
        this.post = post;
        post.getComments().add(this);
    }

    public void changeContent(String content) {
        this.content = content;
    }
}
