package project.board.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.board.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}
