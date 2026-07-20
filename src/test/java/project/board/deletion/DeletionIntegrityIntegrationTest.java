package project.board.deletion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import project.board.comment.entity.Comment;
import project.board.comment.repository.CommentRepository;
import project.board.comment.service.CommentService;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.member.service.MemberService;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;
import project.board.post.service.PostService;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DeletionIntegrityIntegrationTest {

    @Autowired private CommentRepository commentRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CommentService commentService;
    @Autowired private PostService postService;
    @Autowired private MemberService memberService;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void deletingRootCommentRemovesRepliesFirstAndKeepsUnrelatedData() {
        CommentFixture fixture = inTransaction(() -> {
            Member writer = saveMember("comment-writer", "comment-writer@example.com");
            Member owner = saveMember("comment-owner", "comment-owner@example.com");
            Member replier = saveMember("comment-replier", "comment-replier@example.com");
            Post post = postRepository.save(Post.create("comment target", "content", writer));
            Post unrelatedPost = postRepository.save(Post.create("comment unrelated", "content", writer));
            Comment root = commentRepository.save(Comment.create("root", owner, post, null));
            Comment reply = commentRepository.save(Comment.create("reply", replier, post, root));
            Comment unrelated = commentRepository.save(Comment.create("unrelated", replier, unrelatedPost, null));
            return new CommentFixture(owner.getId(), post.getId(), root.getId(), reply.getId(), unrelated.getId(), unrelatedPost.getId());
        });

        inTransaction(() -> commentService.delete(fixture.ownerId(), fixture.rootId(), fixture.postId()));

        assertThat(commentExists(fixture.rootId())).isFalse();
        assertThat(commentExists(fixture.replyId())).isFalse();
        assertThat(commentExists(fixture.unrelatedCommentId())).isTrue();
        assertThat(postExists(fixture.unrelatedPostId())).isTrue();
    }

    @Test
    void userAndAdminPostDeletionRemoveRepliesThenRootsAndKeepUnrelatedPost() {
        PostFixture fixture = inTransaction(() -> {
            Member writer = saveMember("post-writer", "post-writer@example.com");
            Member other = saveMember("post-other", "post-other@example.com");
            Post userPost = postRepository.save(Post.create("user delete", "content", writer));
            Post adminPost = postRepository.save(Post.create("admin delete", "content", writer));
            Post unrelatedPost = postRepository.save(Post.create("post unrelated", "content", other));
            Comment userRoot = commentRepository.save(Comment.create("user root", other, userPost, null));
            Comment userReply = commentRepository.save(Comment.create("user reply", writer, userPost, userRoot));
            Comment adminRoot = commentRepository.save(Comment.create("admin root", writer, adminPost, null));
            Comment adminReply = commentRepository.save(Comment.create("admin reply", other, adminPost, adminRoot));
            Comment unrelated = commentRepository.save(Comment.create("unrelated", writer, unrelatedPost, null));
            return new PostFixture(writer.getId(), userPost.getId(), adminPost.getId(), unrelatedPost.getId(),
                    userRoot.getId(), userReply.getId(), adminRoot.getId(), adminReply.getId(), unrelated.getId());
        });

        inTransaction(() -> postService.delete(fixture.userPostId(), fixture.writerId()));
        inTransaction(() -> postService.deleteForAdmin(fixture.adminPostId()));

        assertThat(postExists(fixture.userPostId())).isFalse();
        assertThat(postExists(fixture.adminPostId())).isFalse();
        assertThat(commentExists(fixture.userRootId())).isFalse();
        assertThat(commentExists(fixture.userReplyId())).isFalse();
        assertThat(commentExists(fixture.adminRootId())).isFalse();
        assertThat(commentExists(fixture.adminReplyId())).isFalse();
        assertThat(postExists(fixture.unrelatedPostId())).isTrue();
        assertThat(commentExists(fixture.unrelatedCommentId())).isTrue();
    }

    @Test
    void memberDeletionRemovesOwnedPostsAndAuthoredCommentGraphOnly() {
        MemberFixture fixture = inTransaction(() -> {
            Member target = saveMember("member-target", "member-target@example.com");
            Member other = saveMember("member-other", "member-other@example.com");
            Post targetPost = postRepository.save(Post.create("target post", "content", target));
            Post otherPost = postRepository.save(Post.create("other post", "content", other));

            Comment targetPostRoot = commentRepository.save(Comment.create("other root on target", other, targetPost, null));
            Comment targetPostReply = commentRepository.save(Comment.create("target reply on target", target, targetPost, targetPostRoot));
            Comment targetRoot = commentRepository.save(Comment.create("target root", target, otherPost, null));
            Comment otherReplyToTarget = commentRepository.save(Comment.create("other reply to target", other, otherPost, targetRoot));
            Comment otherRoot = commentRepository.save(Comment.create("other root", other, otherPost, null));
            Comment targetReply = commentRepository.save(Comment.create("target reply", target, otherPost, otherRoot));
            Comment safeRoot = commentRepository.save(Comment.create("safe root", other, otherPost, null));
            Comment safeReply = commentRepository.save(Comment.create("safe reply", other, otherPost, safeRoot));

            return new MemberFixture(target.getId(), other.getId(), targetPost.getId(), otherPost.getId(),
                    targetPostRoot.getId(), targetPostReply.getId(), targetRoot.getId(), otherReplyToTarget.getId(),
                    otherRoot.getId(), targetReply.getId(), safeRoot.getId(), safeReply.getId());
        });

        inTransaction(() -> memberService.deleteMemberByAdmin(fixture.targetMemberId()));

        assertThat(memberExists(fixture.targetMemberId())).isFalse();
        assertThat(postExists(fixture.targetPostId())).isFalse();
        assertThat(commentExists(fixture.targetPostRootId())).isFalse();
        assertThat(commentExists(fixture.targetPostReplyId())).isFalse();
        assertThat(commentExists(fixture.targetRootId())).isFalse();
        assertThat(commentExists(fixture.otherReplyToTargetId())).isFalse();
        assertThat(commentExists(fixture.targetReplyId())).isFalse();

        assertThat(memberExists(fixture.otherMemberId())).isTrue();
        assertThat(postExists(fixture.otherPostId())).isTrue();
        assertThat(commentExists(fixture.otherRootId())).isTrue();
        assertThat(commentExists(fixture.safeRootId())).isTrue();
        assertThat(commentExists(fixture.safeReplyId())).isTrue();
    }

    @Test
    void downstreamFailureRollsBackOrderedDeletion() {
        CommentFixture fixture = inTransaction(() -> {
            Member writer = saveMember("rollback-writer", "rollback-writer@example.com");
            Member replier = saveMember("rollback-replier", "rollback-replier@example.com");
            Post post = postRepository.save(Post.create("rollback post", "content", writer));
            Comment root = commentRepository.save(Comment.create("rollback root", writer, post, null));
            Comment reply = commentRepository.save(Comment.create("rollback reply", replier, post, root));
            return new CommentFixture(writer.getId(), post.getId(), root.getId(), reply.getId(), root.getId(), post.getId());
        });

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            postService.delete(fixture.postId(), fixture.ownerId());
            throw new IllegalStateException("simulate downstream failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(postExists(fixture.postId())).isTrue();
        assertThat(commentExists(fixture.rootId())).isTrue();
        assertThat(commentExists(fixture.replyId())).isTrue();
    }

    private Member saveMember(String nickname, String email) {
        return memberRepository.save(Member.create(nickname, email, "encoded", Role.USER, LoginType.LOCAL));
    }

    private void inTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    private boolean commentExists(Long id) {
        return inTransaction(() -> commentRepository.existsById(id));
    }

    private boolean postExists(Long id) {
        return inTransaction(() -> postRepository.existsById(id));
    }

    private boolean memberExists(Long id) {
        return inTransaction(() -> memberRepository.existsById(id));
    }

    private record CommentFixture(Long ownerId, Long postId, Long rootId, Long replyId,
                                  Long unrelatedCommentId, Long unrelatedPostId) {}

    private record PostFixture(Long writerId, Long userPostId, Long adminPostId, Long unrelatedPostId,
                               Long userRootId, Long userReplyId, Long adminRootId, Long adminReplyId,
                               Long unrelatedCommentId) {}

    private record MemberFixture(Long targetMemberId, Long otherMemberId, Long targetPostId, Long otherPostId,
                                 Long targetPostRootId, Long targetPostReplyId, Long targetRootId,
                                 Long otherReplyToTargetId, Long otherRootId, Long targetReplyId,
                                 Long safeRootId, Long safeReplyId) {}
}
