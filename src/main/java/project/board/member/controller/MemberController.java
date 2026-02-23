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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.global.security.user.CustomUserDetails;
import project.board.member.dto.MemberDto;
import project.board.member.service.MemberService;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("form", new MemberDto.CreateRequest());
        return "member/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") MemberDto.CreateRequest form,
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

    @GetMapping("/members/me")
    public String myProfile(@AuthenticationPrincipal CustomUserDetails user,
                            Model model) {
        model.addAttribute("profile", memberService.getMyProfile(user.getMemberId()));
        return "member/me";
    }

//    @GetMapping("/member/me/edit")
//    public String editForm(@AuthenticationPrincipal CustomUserDetails user,
//                           Model model) {
//
//    }
}
