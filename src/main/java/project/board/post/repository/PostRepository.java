package project.board.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.board.post.dto.PostDto;
import project.board.post.entity.Post;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {


    List<Post> findByTitleContaining(String keyword);
}
