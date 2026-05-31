package project.board.post.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import project.board.global.security.config.JpaConfig;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.dto.request.PostRecent;
import project.board.post.entity.Post;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("fetches post list with writer and searches by title")
    void findAllWithMemberAndSearch() {
        Member writer = saveMember("writer", "writer@example.com");
        postRepository.save(Post.create("Spring board", "content1", writer));
        postRepository.save(Post.create("JPA board", "content2", writer));
        flushAndClear();

        Page<Post> page = postRepository.findAllWithMember(PageRequest.of(0, 10));
        List<Post> searched = postRepository.findByTitleContaining("Spring");

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Post::getTitle)
                .containsExactlyInAnyOrder("Spring board", "JPA board");
        assertThat(page.getContent()).allSatisfy(post -> assertThat(post.getMember().getNickname()).isEqualTo("writer"));
        assertThat(searched).extracting(Post::getTitle).containsExactly("Spring board");
    }

    @Test
    @DisplayName("finds my posts, recent posts and count queries")
    void findMyPostsAndCounts() {
        Member writer = saveMember("writer", "writer@example.com");
        Member other = saveMember("other", "other@example.com");
        postRepository.save(Post.create("my post 1", "content1", writer));
        postRepository.save(Post.create("my post 2", "content2", writer));
        postRepository.save(Post.create("other post", "content3", other));
        flushAndClear();

        Long todayCount = postRepository.countTodayPosts(
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
        );
        List<Post> myPosts = postRepository.findAllByMemberId(writer.getId());
        Long myPostCount = postRepository.countMyPosts(writer.getId());
        List<PostRecent> recentPosts = postRepository.findMyRecentPosts(writer.getId(), PageRequest.of(0, 1));

        assertThat(todayCount).isEqualTo(3);
        assertThat(myPosts).hasSize(2);
        assertThat(myPostCount).isEqualTo(2);
        assertThat(recentPosts).hasSize(1);
        assertThat(recentPosts.get(0).getTitle()).startsWith("my post");
    }

    @Test
    @DisplayName("increments view count and bulk deletes by member")
    void incrementViewCountAndDeleteAllByMemberId() {
        Member writer = saveMember("writer", "writer@example.com");
        Post post = postRepository.save(Post.create("view post", "content", writer));
        flushAndClear();

        int updated = postRepository.incrementViewCount(post.getId());
        flushAndClear();

        assertThat(updated).isEqualTo(1);
        assertThat(postRepository.findById(post.getId())).get()
                .extracting(Post::getViewCount)
                .isEqualTo(1);

        postRepository.deleteAllByMemberId(writer.getId());
        flushAndClear();

        assertThat(postRepository.findAllByMemberId(writer.getId())).isEmpty();
    }

    private Member saveMember(String nickname, String email) {
        return memberRepository.save(Member.create(nickname, email, "encoded", Role.USER, LoginType.LOCAL));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
