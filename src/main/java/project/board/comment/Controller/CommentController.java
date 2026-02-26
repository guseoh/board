package project.board.comment.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.comment.dto.CommentDto;
import project.board.comment.dto.CommentRequestDto;
import project.board.comment.service.CommentService;
import project.board.global.security.user.CustomUserDetails;
import project.board.post.dto.PostDto;
import project.board.post.service.PostService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;

    @GetMapping("/post/{id}")
    public String createForm(@PathVariable Long postId,
                             Model model) {
        PostDto.Response post = postService.findOne(postId);
        List<CommentDto.Response> comments = commentService.findAll(postId);

        return "post/detail";
    }

    @PostMapping("/post/{id}/comment")
    public String create(@PathVariable Long postId,
                         @ModelAttribute("commentForm") @Valid CommentRequestDto requestDto,
                         @AuthenticationPrincipal CustomUserDetails user,
                         BindingResult bindingResult,
                         RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "댓글 내용을 확인해주세요");
            return "redirect:/post/" + postId;
        }

        commentService.create(requestDto, postId, user.getMemberId());

        return "redirect:/post/" + postId;
    }

    @PostMapping("/post/{id}/comment/{id}/edit")
    public String update(@PathVariable Long postId,
                         @PathVariable Long commentId,
                         @Valid @ModelAttribute("commentUpdateForm") CommentRequestDto commentRequestDto,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails customUserDetails,
                         RedirectAttributes ra) {

    }
}
