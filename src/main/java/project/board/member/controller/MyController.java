package project.board.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.comment.dto.MyCommentPageResponse;
import project.board.comment.dto.MyRecentComment;
import project.board.comment.service.CommentService;
import project.board.global.dto.PageRequestDto;
import project.board.global.security.user.UnifiedPrincipal;
import project.board.member.dto.request.MemberNicknameUpdateRequest;
import project.board.member.dto.request.MemberPasswordUpdateRequest;
import project.board.member.dto.request.MemberUpdateRequest;
import project.board.member.dto.response.MemberUpdateResponse;
import project.board.member.service.MemberService;
import project.board.post.dto.request.PostRecent;
import project.board.post.service.PostService;

import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class MyController {

    private final MemberService memberService;
    private final PostService postService;
    private final CommentService commentService;


    @GetMapping("/my")
    public String myForm(Model model, @AuthenticationPrincipal UnifiedPrincipal user) {

        Long memberId = user.getMemberId();

        model.addAttribute("myPostCount", postService.myPostCount(memberId));

        model.addAttribute("myCommentCount", commentService.myCommentCount(memberId));

        model.addAttribute("recentPosts", postService.recentPosts(memberId));

        model.addAttribute("recentComments", commentService.recentComments(memberId));

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
                                Model model,
                                @AuthenticationPrincipal UnifiedPrincipal user) {

        MyCommentPageResponse pageResponse = commentService.myCommentPage(user.getMemberId(), request, keyword);

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

        log.info("회원 탈퇴 성공 - memberId={}, nickName={}", user.getMemberId(), user.getNickname());

        return "redirect:/";
    }

//    @GetMapping("/my/edit")
//    public String EditForm(@AuthenticationPrincipal UnifiedPrincipal user,
//                           Model model) {
//
//        MemberUpdateRequest form = memberService.getMyProfile(user.getMemberId());
//
//        model.addAttribute("form", form);
//
//        return "my/myEdit";
//    }

    @GetMapping("/my/edit")
    public String EditForm(@AuthenticationPrincipal UnifiedPrincipal user, Model model) {

        MemberUpdateResponse form = memberService.getMyProfile(user.getMemberId());

        MemberNicknameUpdateRequest nicknameRequest = new MemberNicknameUpdateRequest();
        nicknameRequest.setNickname(form.getNickname());


        model.addAttribute("form", form);
        model.addAttribute("nicknameRequest", nicknameRequest);
        model.addAttribute("passwordRequest", new MemberPasswordUpdateRequest());

        return "my/myEdit";
    }

    @PostMapping("/my/edit/nickname")
    public String EditNickname(@Valid @ModelAttribute("form") MemberNicknameUpdateRequest request,
                        BindingResult bindingResult,
                       RedirectAttributes ra,
                       @AuthenticationPrincipal UnifiedPrincipal user) {

        if (bindingResult.hasErrors()) {
            return "my/myEdit";
        }

        MemberUpdateResponse memberUpdateResponse = memberService.updateNickname(user.getMemberId(), request);

        ra.addFlashAttribute("msg", "닉네임이 수정되었습니다.");

        log.info("회원 닉네임 변경 성공! - memberId={}, Nickname{} ", user.getMemberId(), user.getNickname());

        refreshAuthentication(memberUpdateResponse.getNickname(), user);

        return "redirect:/";
    }

    private void refreshAuthentication(String newNickname, UnifiedPrincipal currentUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UnifiedPrincipal newUser = UnifiedPrincipal.builder()
                .memberId(currentUser.getMemberId())
                .email(currentUser.getEmail())
                .nickname(newNickname)
                .role(currentUser.getRole())
                .loginType(currentUser.getLoginType())
                .build();

        Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                newUser,
                authentication.getCredentials(),
                authentication.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(newAuthentication);
    }

    @PostMapping("/my/edit/password")
    public String EditPassword(@Valid @ModelAttribute("form") MemberPasswordUpdateRequest request,
                        BindingResult bindingResult,
                       RedirectAttributes ra,
                       @AuthenticationPrincipal UnifiedPrincipal user) {

        if (bindingResult.hasErrors()) {
            return "my/myEdit";
        }

        memberService.updatePassword(user.getMemberId(), request);

        ra.addFlashAttribute("msg", "비밀번호가 수정되었습니다.");

        log.info("회원 비밀번호 변경 성공! - memberId={}, Nickname{} ", user.getMemberId(), user.getNickname());

        return "redirect:/";
    }
}
