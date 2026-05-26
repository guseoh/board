package project.board.comment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.board.global.entity.BaseEntity;
import project.board.member.entity.Member;
import project.board.post.entity.Post;

import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent")
    private List<Comment> children = new ArrayList<>();


    public static Comment create(String content, Member member, Post post, Comment parent) {

        Comment c = new Comment();
        c.content = content;
        c.member = member;

        post.addComment(c);

        if (parent != null) {
            parent.addChild(c);
        }

        return c;
    }

    private void addChild(Comment c) {
        this.children.add(c);
        c.parent = this;
    }

    public void addPost(Post post) {
        this.post = post;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public boolean rootComment() {
        return parent == null;
    }

    public boolean isReply() {
        return parent != null;
    }
}
