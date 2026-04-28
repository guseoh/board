package project.board.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.comment.dto.MyCommentPageResponse;
import project.board.comment.service.CommentService;
import project.board.global.dto.PageRequestDto;
import project.board.global.security.user.UnifiedPrincipal;
import project.board.member.dto.request.MemberCreateRequest;
import project.board.member.service.MemberService;
import project.board.post.service.PostService;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final PostService postService;
    private final MemberService memberService;
    private final CommentService commentService;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("form", new MemberCreateRequest());
        return "member/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") MemberCreateRequest form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "member/signup";
        }

        memberService.signUp(form);

        redirectAttributes.addFlashAttribute("msg", "회원가입 완료");

        return "redirect:/loginForm";
    }

    @GetMapping("/loginForm")
    public String loginForm() {
        return "member/loginForm";
    }


    @GetMapping("/my")
    public String myForm(Model model, RedirectAttributes ra,
                         @AuthenticationPrincipal UnifiedPrincipal user) {

        Long postCount = postService.myPostCount(user.getMemberId());
        model.addAttribute("myPostCount", postCount);

        Long commentCount = commentService.myCommentCount(user.getMemberId());
        model.addAttribute("myCommentCount", commentCount);

        return "member/my";

    }

//    // 회원정보 수정
//    @GetMapping("/my/edit")
//    public String myEditForm(Model model) {
//
//    }
//
//    // 비밀번호 변경
//    @GetMapping("/my/password")
//    public String myPasswordForm() {
//
//    }
//
//


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

        return "member/myPost";
    }

//    @GetMapping("/my/comments")
//    public String myCommentForm(PageRequestDto requestDto,
//            @RequestParam(required = false) String keyword,
//            Model model) {
//
//        MyCommentPageResponse response = commentService.




//        model.addAttribute("myCommentCount", myCommentCount);
//        model.addAttribute("todayMyCommentCount", todayMyCommentCount);
//        model.addAttribute("recentCommentCount", recentCommentCount);
//
//        model.addAttribute("comments", comments);
//        model.addAttribute("page", page);
//        model.addAttribute("keyword", keyword);


        return "member/myComment";

    }
}
