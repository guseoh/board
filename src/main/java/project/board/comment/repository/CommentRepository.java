package project.board.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.board.comment.entity.Comment;
import project.board.member.entity.Member;

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

    @Query("select count(c) from Comment c where c.member = :member")
    Long countMyComments(@Param("member") Member member);
}
