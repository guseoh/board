package project.board.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.board.comment.entity.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPostIdOrderByIdAsc(Long postId);
}
