package project.board.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.board.post.dto.request.PostRecent;
import project.board.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByTitleContaining(String keyword);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    //todo: 아래 메서드와 비교
    @Query("select p from Post p join fetch p.member")
    Page<Post> findAllWithMember(Pageable pageable);

    @Query("select p from Post p join fetch p.member")
    List<Post> findAllWithMemberForAdmin();

    @Query("select count(p) from Post p where p.createdAt >= :startDay and p.createdAt < :nextDay")
    Long countTodayPosts(@Param("startDay") LocalDateTime startDay, @Param("nextDay") LocalDateTime nextDay);


    @Query("select count(p) from Post p where p.member.id = :memberId")
    Long countMyPosts(@Param("memberId") Long memberId);

    @Query("select coalesce(sum(p.viewCount), 0) from Post p where p.member.id = :memberId")
    Long sumViewCountByMemberId(@Param("memberId") Long memberId);

    Long countByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long memberId, LocalDateTime startDay, LocalDateTime nextDay);

    List<Post> findAllByMemberId(Long memberId);

    @Query(value = """
            select p from Post p
            join fetch p.member
            where p.member.id = :memberId
              and (:keyword is null or :keyword = '' or p.title like concat('%', :keyword, '%'))
            """,
            countQuery = """
            select count(p) from Post p
            where p.member.id = :memberId
              and (:keyword is null or :keyword = '' or p.title like concat('%', :keyword, '%'))
            """)
    Page<Post> findMyPosts(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Modifying
    @Query("delete from Post p where p.member.id = :memberId")
    void deletePostsByMemberId(@Param("memberId") Long memberId);


    // 제목, 조회수, 생성일자
    @Query("""
                select new project.board.post.dto.request.PostRecent(
                p.id, p.title, p.viewCount, p.createdAt
                )
                from Post p
                where p.member.id = :memberId
                order by p.id desc
            """)
    List<PostRecent> findMyRecentPosts(@Param("memberId") Long memberId, Pageable pageable);



}
