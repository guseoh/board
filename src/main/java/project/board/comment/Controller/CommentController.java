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

    @PostMapping("/post/{postId}/comment")
    public String create(@PathVariable Long postId,
                         @ModelAttribute("commentForm") @Valid CommentRequestDto requestDto,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails user,
                         RedirectAttributes ra) {

        if (user == null) {
            ra.addAttribute("redirect", "/post/" + postId);
            return "redirect:/loginForm";
        }

        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "댓글 내용을 확인해주세요");
            return "redirect:/post/" + postId;
        }

        commentService.create(requestDto, user.getMemberId(), postId);

        return "redirect:/post/" + postId;
    }

    @PostMapping("/post/{postId}/comment/{commentId}/edit")
    public String update(@PathVariable Long postId,
                         @PathVariable Long commentId,
                         @Valid @ModelAttribute("commentUpdateForm") CommentRequestDto commentRequestDto,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails customUserDetails,
                         RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "댓글 내용을 확인해주세요");
            return "redirect:/post/" + postId;
        }

        commentService.update(commentId, customUserDetails.getMemberId(), postId, commentRequestDto);

        return "redirect:/post/" + postId;

    }

    @PostMapping("/post/{postId}/comment/{commentId}/delete")
    public String delete(@PathVariable Long postId,
                         @PathVariable Long commentId,
                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        commentService.delete(customUserDetails.getMemberId(), commentId, postId);


        return "redirect:/post/" + postId;
    }
}
