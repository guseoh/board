package project.board.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.board.comment.dto.response.MyCommentResponse;
import project.board.comment.dto.response.MyRecentCommentResponse;
import project.board.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying
    @Query("delete from Comment c where c.parent.id = :parentId")
    void deleteRepliesByParentId(@Param("parentId") Long parentId);

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId and c.parent is not null")
    void deleteRepliesByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId and c.parent is null")
    void deleteRootCommentsByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("delete from Comment c where c.post.member.id = :memberId and c.parent is not null")
    void deleteRepliesByPostMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from Comment c where c.post.member.id = :memberId and c.parent is null")
    void deleteRootCommentsByPostMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from Comment c where c.parent.member.id = :memberId")
    void deleteRepliesToRootsByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from Comment c where c.member.id = :memberId and c.parent is not null")
    void deleteRepliesByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from Comment c where c.member.id = :memberId and c.parent is null")
    void deleteRootCommentsByMemberId(@Param("memberId") Long memberId);

    // 전체 작성 댓글 (내가 쓴)
    long countByMemberId(Long memberId);

    // 오늘 작성한 댓글 개수
    Long countByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long memberId, LocalDateTime today, LocalDateTime nextDay
    );

    // 최근 7일 댓글
    Long countByMemberIdAndCreatedAtGreaterThanEqual(
            Long memberId, LocalDateTime fromDateTime);


    @Query("""
                select new project.board.comment.dto.response.MyCommentResponse(
                    c.id, p.id, p.title, c.content, c.createdAt
                )
                from Comment c
                join c.post p
                where c.member.id = :memberId
                and (
                       :keyword is null
                       or :keyword = ''
                       or c.content like concat('%', :keyword, '%')
                       or p.title like concat('%', :keyword, '%')
                )
           """)
    Page<MyCommentResponse> findMyComments(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            Pageable pageable
    );


    @Query("""
        select new project.board.comment.dto.response.MyRecentCommentResponse(
            c.id,
            p.title,
            c.content,
            c.createdAt
        )
        from Comment c
        join c.post p
        where c.member.id = :memberId
        order by c.id desc
        """
    )
    List<MyRecentCommentResponse> findRecentComments(
            @Param("memberId") Long memberId,
            Pageable pageable
    );
}
