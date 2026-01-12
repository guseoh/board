package project.board.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.board.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
