package project.board.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.board.comment.entity.Comment;
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

    //todo: 개선
    @Query("select count(c) from Comment c where c.member = :member")
    Long countMyComments(@Param("member") Member member);

    @Query("select count(c) from Comment c where c.member.id = :commentId and c.createdAt >= :today and c.createdAt < :nextDay")
    Long countTodayMyComments(@Param("commentId") Long commentId,
                              @Param("today") LocalDateTime today,
                              @Param("nextDay") LocalDateTime nextDay);

    @Query("select count(c) from Comment c where c.member.id = :memberId and c.createdAt >= :minusDay")
    Long countRecentComments(@Param("memberId") Long memberId,
                               @Param("minusDay") LocalDateTime minusDay);
///
///

//    @Query("select c from Comment c where c.member.id = :memberId")
//    List<Comment> MyComments(@Param("memberId") Long memberId);

    // 전체 작성 댓글 (내가 쓴)
    Long countByMemberId(Long memberId);

    // 오늘 작성한 댓글 개수
    Long countByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long memberId, LocalDateTime today, LocalDateTime nextDay
    );

}
