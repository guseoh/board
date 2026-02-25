package project.board.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.board.post.entity.Post;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByTitleContaining(String keyword);
}
