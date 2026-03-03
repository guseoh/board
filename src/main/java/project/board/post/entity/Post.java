package project.board.post.entity;

import jakarta.persistence.*;
import lombok.*;
import project.board.comment.entity.Comment;
import project.board.global.entity.BaseEntity;
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
        Post p = new Post();
        p.title = title;
        p.content = content;
        p.assignMember(member);
        return p;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.addPost(this);
    }

    public void assignMember(Member member) {
        this.member = member;
        member.getPosts().add(this);
    }

    public void change(String title, String content) {
        this.title = title;
        this.content = content;
    }

//    public void increaseViewCount() {
//        this.viewCount++;
//    }
}
