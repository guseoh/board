package project.board.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.board.comment.dto.MyCommentResponse;
import project.board.comment.entity.Comment;
import project.board.global.dto.PageResultDto;
import project.board.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPostIdOrderByIdAsc(Long postId);

    @Modifying
    @Query("delete from Comment c where c.post.id = :id")
    void deleteById(@Param("id") Long postId);

    @Modifying
    @Query("delete from Comment c where c.member = :member")
    void deleteAllByMember(@Param("member") Member member);

    @Modifying
    @Query("delete from Comment c where c.post.member = :member")
    void deleteAllByPostMember(@Param("member") Member member);

    // 전체 작성 댓글 (내가 쓴)
    long countByMemberId(Long memberId);

    // 오늘 작성한 댓글 개수
    Long countByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long memberId, LocalDateTime today, LocalDateTime nextDay
    );

    // 최근 7일 댓글
    Long countByMemberIdAndCreatedAtGreaterThanEqual(
            Long memberId, LocalDateTime fromDateTime);



}
