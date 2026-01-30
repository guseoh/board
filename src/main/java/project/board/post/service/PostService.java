package project.board.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;
import project.board.post.dto.PostDto;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;    // nullable = false

    @Transactional
    public PostDto.Response save(PostDto.CreateRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new IllegalArgumentException("해당 사용자는 존재하지 않습니다."));

        Post saved = postRepository.save(Post.create(request.getTitle(), request.getContent(), member));

//        Post saved = postRepository.save(PostDto.CreateRequest.toEntity(request));

        return PostDto.Response.from(saved);
    }

    public PostDto.Response findOne(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        return PostDto.Response.from(post);
    }

    public Page<PostDto.Response> findAll(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(PostDto.Response::from);   //todo:
    }

    @Transactional
    public void update(PostDto.UpdateRequest request, Long postId, Long memberId) {
        Post post = postRepository.findById(postId).orElseThrow(() ->
                new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        if (!post.getId().equals(memberId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        //todo: 통합으로 개선
        post.changeTitle(request.getTitle());
        post.changeContent(request.getContent());
    }

    @Transactional
    public void delete(Long id) {
        Post find = postRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        postRepository.delete(find);
    }

}
