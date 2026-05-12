package project.board.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.board.comment.dto.MyCommentPageResponse;
import project.board.comment.service.CommentService;
import project.board.global.dto.PageRequestDto;
import project.board.global.security.user.UnifiedPrincipal;
import project.board.member.service.MemberService;
import project.board.post.service.PostService;

@Controller
@Slf4j
@RequiredArgsConstructor
public class MyController {

    private final MemberService memberService;
    private final PostService postService;
    private final CommentService commentService;


    @GetMapping("/my")
    public String myForm(Model model, @AuthenticationPrincipal UnifiedPrincipal user) {

        Long postCount = postService.myPostCount(user.getMemberId());
        model.addAttribute("myPostCount", postCount);

        Long commentCount = commentService.myCommentCount(user.getMemberId());
        model.addAttribute("myCommentCount", commentCount);

        return "my/my";

    }
    @GetMapping("/my/posts")
    public String myPostsForm(PageRequestDto pageRequestDto,
                              Model model,
                              @AuthenticationPrincipal UnifiedPrincipal user) {

        var page = postService.findAll(pageRequestDto);

        model.addAttribute("myPostCount", postService.myPostCount(user.getMemberId()));

        model.addAttribute("todayMyPostCount", postService.todayWrite());
        model.addAttribute("myPostViewCount", postService.myTodayPostsCount(user.getMemberId()));

        model.addAttribute("posts", postService.myPosts(user.getMemberId()));
        model.addAttribute("page", page);
        model.addAttribute("keyword", pageRequestDto.getKeyword());

        return "my/myPost";
    }

    @GetMapping("/my/comments")
    public String myCommentForm(PageRequestDto request,
                                @RequestParam(required = false) String keyword,
                                Model model) {

        MyCommentPageResponse pageResponse = commentService.myCommentPage(request, keyword);

        model.addAttribute("pageResponse", pageResponse);

        return "my/myComment";

    }

    @GetMapping("/my/withdraw")
    public String withdrawForm() {
        return "my/withdraw";
    }

    @PostMapping("/my/withdraw")
    public String withdraw(@AuthenticationPrincipal UnifiedPrincipal user,
                           HttpServletRequest request,
                           HttpServletResponse response) {

        memberService.withdraw(user.getMemberId());

        new SecurityContextLogoutHandler().logout(request, response, null);

        return "redirect:/";
    }
}
