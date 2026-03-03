package project.board.post.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostServiceTest {

    @Autowired
    private PostService postService;
    @Autowired
    PostRepository postRepository;
    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("중복 조회: viewCount()를 여러 번 호출하면 그만큼 조회수가 증가하는지")
    @Transactional
    void 조회수_테스트() {

        // given
        Member member = memberRepository.save(Member.create("닉네임", "3131@t.com", "1111", Role.USER));

        Post post = postRepository.save(Post.create("제목", "내용", member));

        // when
        postService.viewCount(post.getId());
        postService.viewCount(post.getId());
        postService.viewCount(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        Assertions.assertThat(updated.getViewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("조회수 증가 정합성 테스트: 동시 1000회")
    void 조회수_증가_정합성() throws InterruptedException{

        // given
        Member member = memberRepository.save(Member.create("닉네임", "aaaa@t.com", "1111", Role.USER));
        Post post = postRepository.save(Post.create("제목", "내용", member));

        int count = 1000;

        ExecutorService ex = Executors.newFixedThreadPool(64);
        CountDownLatch latch = new CountDownLatch(count);

        // when
        for (int i = 0; i < count; i++) {
            ex.execute(() -> {
                try{
                    postService.viewCount(post.getId());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        Post updated = postRepository.findById(post.getId()).orElseThrow();
        System.out.println("최종 조회수 = " + updated.getViewCount());

        Assertions.assertThat(updated.getViewCount()).isEqualTo(count);
    }
}