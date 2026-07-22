package project.board.post.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import project.board.global.pagination.PageRequestDto;
import project.board.global.security.principal.UnifiedPrincipal;
import project.board.post.dto.request.PostCreateApiRequest;
import project.board.post.dto.request.PostUpdateApiRequest;
import project.board.post.dto.response.PostCreateApiResponse;
import project.board.post.dto.response.PostDetailApiResponse;
import project.board.post.dto.response.PostPageApiResponse;
import project.board.post.service.PostService;

import java.net.URI;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<PostPageApiResponse> getPosts(
            @Valid @ModelAttribute PageRequestDto request
    ) {
        var pageResult = postService.getPosts(request);

        return ResponseEntity.ok(
                PostPageApiResponse.from(pageResult)
        );
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailApiResponse> getPost(
            @PathVariable Long postId
    ) {
        PostDetailApiResponse response = postService.getPostApiDetail(postId);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PostCreateApiResponse> createPost(
            @Valid @RequestBody PostCreateApiRequest request,
            @AuthenticationPrincipal UnifiedPrincipal user
    ) {
        Long postId = postService.createPost(request, user.getMemberId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{postId}")
                .buildAndExpand(postId)
                .toUri();

        return ResponseEntity
                .created(location)
                .body(new PostCreateApiResponse(postId));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateApiRequest request,
            @AuthenticationPrincipal UnifiedPrincipal user
    ) {
        postService.update(request, postId, user.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UnifiedPrincipal user
    ) {
        postService.delete(postId, user.getMemberId());

        return ResponseEntity.noContent().build();
    }
}

//todo: ModelAttribute, URI 이유, updatePost 204 반환 이유