package project.board.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.comment.service.CommentService;
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



}
