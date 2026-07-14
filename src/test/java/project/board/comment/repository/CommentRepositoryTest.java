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
    @DisplayName("게시글 댓글은 답글을 먼저 삭제하고 bulk delete 후 영속성 컨텍스트를 비운다")
    void deletePostCommentsInHierarchyOrder() {
        Member writer = saveMember("writer", "writer@example.com");
        Member commenter = saveMember("commenter", "commenter@example.com");
        Post post = postRepository.save(Post.create("post", "content", writer));
        Comment root = commentRepository.save(Comment.create("root", commenter, post, null));
        Comment reply = commentRepository.save(Comment.create("reply", writer, post, root));
        flushAndClear();

        Comment managedRoot = commentRepository.findById(root.getId()).orElseThrow();
        assertThat(entityManager.contains(managedRoot)).isTrue();

        assertThat(commentRepository.deleteRepliesByPostId(post.getId())).isEqualTo(1);
        assertThat(entityManager.contains(managedRoot)).isFalse();
        assertThat(commentRepository.deleteRootCommentsByPostId(post.getId())).isEqualTo(1);

        assertThat(commentRepository.findById(root.getId())).isEmpty();
        assertThat(commentRepository.findById(reply.getId())).isEmpty();
    }

    @Test
    @DisplayName("회원 댓글 삭제는 부모의 답글, 회원 답글, 회원 부모 댓글 순서를 지원한다")
    void deleteMemberCommentsInHierarchyOrder() {
        Member retiring = saveMember("retiring", "retiring@example.com");
        Member other = saveMember("other", "other@example.com");
        Post post = postRepository.save(Post.create("other post", "content", other));
        Comment retiringRoot = commentRepository.save(Comment.create("retiring root", retiring, post, null));
        Comment replyToRetiring = commentRepository.save(Comment.create("other reply", other, post, retiringRoot));
        Comment otherRoot = commentRepository.save(Comment.create("other root", other, post, null));
        Comment retiringReply = commentRepository.save(Comment.create("retiring reply", retiring, post, otherRoot));
        flushAndClear();

        assertThat(commentRepository.deleteRepliesToCommentsByMemberId(retiring.getId())).isEqualTo(1);
        assertThat(commentRepository.deleteRepliesByMemberId(retiring.getId())).isEqualTo(1);
        assertThat(commentRepository.deleteRootCommentsByMemberId(retiring.getId())).isEqualTo(1);

        assertThat(commentRepository.findById(retiringRoot.getId())).isEmpty();
        assertThat(commentRepository.findById(replyToRetiring.getId())).isEmpty();
        assertThat(commentRepository.findById(retiringReply.getId())).isEmpty();
        assertThat(commentRepository.findById(otherRoot.getId())).isPresent();
    }

    @Test
    @DisplayName("회원이 작성한 게시글의 댓글은 답글과 부모 댓글 순서로 삭제한다")
    void deleteCommentsOnMemberPostsInHierarchyOrder() {
        Member retiring = saveMember("retiring", "retiring@example.com");
        Member other = saveMember("other", "other@example.com");
        Post post = postRepository.save(Post.create("retiring post", "content", retiring));
        Comment root = commentRepository.save(Comment.create("root", other, post, null));
        Comment reply = commentRepository.save(Comment.create("reply", other, post, root));
        flushAndClear();

        assertThat(commentRepository.deleteRepliesByPostMemberId(retiring.getId())).isEqualTo(1);
        assertThat(commentRepository.deleteRootCommentsByPostMemberId(retiring.getId())).isEqualTo(1);

        assertThat(commentRepository.findById(root.getId())).isEmpty();
        assertThat(commentRepository.findById(reply.getId())).isEmpty();
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
