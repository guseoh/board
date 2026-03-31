package project.board.post.repository;

import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.board.member.entity.Member;
import project.board.post.entity.Post;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByTitleContaining(String keyword);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    @Query("select p from Post p join fetch p.member")
    Page<Post> findAllWithMember(Pageable pageable);

    @Query("select p from Post p join fetch p.member")
    List<Post> findAllWithMemberForAdmin();


    @Modifying
    @Query("delete from Post p where p.member = :member")
    void deleteAllByMember(@Param("member") Member member);

}
