package project.board.post.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.board.global.pagination.PageRequestDto;
import project.board.post.dto.response.PostDetailApiResponse;
import project.board.post.dto.response.PostPageApiResponse;
import project.board.post.service.PostService;

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
}

//todo: ModelAttribute