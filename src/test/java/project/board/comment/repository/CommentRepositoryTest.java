package project.board.comment.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import project.board.comment.dto.response.MyCommentResponse;
import project.board.comment.dto.response.MyRecentCommentResponse;
import project.board.comment.entity.Comment;
import project.board.global.security.config.JpaConfig;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("답글과 부모 댓글을 명시적인 대상별 쿼리로 삭제한다")
    void deleteQueries() {
        Member writer = saveMember("writer", "writer@example.com");
        Member commenter = saveMember("commenter", "commenter@example.com");
        Member other = saveMember("other", "other@example.com");
        Post post = postRepository.save(Post.create("post", "content", writer));
        Comment root = commentRepository.save(Comment.create("root", commenter, post, null));
        Comment reply = commentRepository.save(Comment.create("reply", other, post, root));
        flushAndClear();

        commentRepository.deleteRepliesByParentId(root.getId());
        flushAndClear();
        assertThat(commentRepository.findById(reply.getId())).isEmpty();
        assertThat(commentRepository.findById(root.getId())).isPresent();

        Comment secondRoot = commentRepository.save(Comment.create("second root", commenter, post, null));
        Comment secondReply = commentRepository.save(Comment.create("second reply", other, post, secondRoot));
        flushAndClear();
        commentRepository.deleteRepliesByPostId(post.getId());
        commentRepository.deleteRootCommentsByPostId(post.getId());
        flushAndClear();
        assertThat(commentRepository.findById(secondReply.getId())).isEmpty();
        assertThat(commentRepository.findById(root.getId())).isEmpty();
        assertThat(commentRepository.findById(secondRoot.getId())).isEmpty();
    }

    @Test
    @DisplayName("내 댓글, 최근 댓글, 개수 쿼리를 조회한다")
    void myCommentQueries() {
        Member writer = saveMember("writer", "writer@example.com");
        Member commenter = saveMember("commenter", "commenter@example.com");
        Post post = postRepository.save(Post.create("searchable post", "content", writer));
        commentRepository.save(Comment.create("searchable comment", commenter, post, null));
        commentRepository.save(Comment.create("other comment", writer, post, null));
        flushAndClear();

        Page<MyCommentResponse> page = commentRepository.findMyComments(
                commenter.getId(),
                "searchable",
                PageRequest.of(0, 10)
        );
        List<MyRecentCommentResponse> recentComments = commentRepository.findRecentComments(
                commenter.getId(),
                PageRequest.of(0, 5)
        );
        Long todayCount = commentRepository.countByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                commenter.getId(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
        );
        Long sevenDaysCount = commentRepository.countByMemberIdAndCreatedAtGreaterThanEqual(
                commenter.getId(),
                LocalDate.now().minusDays(7).atStartOfDay()
        );

        assertThat(commentRepository.countByMemberId(commenter.getId())).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getPostTitle()).isEqualTo("searchable post");
        assertThat(recentComments).hasSize(1);
        assertThat(recentComments.get(0).getTitle()).isEqualTo("searchable post");
        assertThat(todayCount).isEqualTo(1);
        assertThat(sevenDaysCount).isEqualTo(1);
    }

    private Member saveMember(String nickname, String email) {
        return memberRepository.save(Member.create(nickname, email, "encoded", Role.USER, LoginType.LOCAL));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
