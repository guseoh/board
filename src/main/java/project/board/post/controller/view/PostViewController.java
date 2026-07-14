package project.board.post.controller.view;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.comment.dto.request.CommentCreateRequest;
import project.board.comment.service.CommentService;
import project.board.global.pagination.PageRequestDto;
import project.board.global.security.principal.UnifiedPrincipal;
import project.board.member.service.MemberService;
import project.board.post.dto.request.PostRequest;
import project.board.post.dto.response.PostDetailResponse;
import project.board.post.dto.response.PostListResponse;
import project.board.post.service.PostService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PostViewController {

    private final MemberService memberService;
    private final PostService postService;
    private final CommentService commentService;

    @GetMapping({"", "/"})
    public String list(PageRequestDto request, Model model,
                       @AuthenticationPrincipal UnifiedPrincipal user) {

        var page = postService.getPosts(request);

        model.addAttribute("totalCount", page.getTotalCount());
        model.addAttribute("todayCount", postService.countTodayPosts());
        model.addAttribute("memberCount", memberService.countMember());

        if (user != null) {
            model.addAttribute("myPostCount", postService.countMyPosts(user.getMemberId()));
            model.addAttribute("myCommentCount", commentService.countMyComment(user.getMemberId()));

        }

        model.addAttribute("page", page);
        model.addAttribute("posts", page.getDtoList());

        return "post/list";
    }

    @GetMapping("/post/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal UnifiedPrincipal customUserDetails,
                         Model model,
                         HttpServletRequest request,
                         HttpServletResponse response) {

        if (increaseViewCount(id, request, response)) {
            postService.viewCount(id);
        }

        PostDetailResponse post = postService.getPostDetail(id);

        model.addAttribute("post", post);
        model.addAttribute("comments", post.getComments());
        model.addAttribute("commentForm", new CommentCreateRequest());
        model.addAttribute("memberId", customUserDetails != null ? customUserDetails.getMemberId() : null);

        return "post/detail";
    }

    private boolean increaseViewCount(Long id, HttpServletRequest request, HttpServletResponse response) {

        final String cookieName = "View_Count";
        final String token = "|" + id + "|";
        final int time = 60 * 60 * 12;

        Cookie cookie = null;
        Cookie[] cookies = request.getCookies();

        // "View_Count" 이름을 가진 쿠키를 찾기
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (cookieName.equals(c.getName())) {
                    cookie = c;
                    break;
                }
            }
        }

        // 쿠키가 없으면 새로 생성하고 증가
        if (cookie == null) {
            Cookie newCookie = new Cookie(cookieName, token);
            newCookie.setPath("/");
            newCookie.setMaxAge(time);
            newCookie.setHttpOnly(true);
            response.addCookie(newCookie);
            return true;
        }

        String value = cookie.getValue();

        if (value != null && value.contains(token)) {
            return false;
        }

        String updated = (value == null ? "" : value) + token;
        cookie.setValue(updated);
        cookie.setPath("/");
        cookie.setMaxAge(time);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return true;
    }

    @GetMapping("/posts/search")
    public String search(@RequestParam String keyword, Model model) {
        List<PostListResponse> posts = postService.search(keyword);
        model.addAttribute("posts", posts);
        model.addAttribute("keyword", keyword);

        return "post/list";
    }

    @PostMapping("/post/new")
    public String create(@Valid @ModelAttribute("form") PostRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UnifiedPrincipal customUserDetails,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.warn("게시글 등록 검증 실패 - memberId={}, title ={}, errorCount={}, errors={}",
                    customUserDetails != null ? customUserDetails.getMemberId() : null,
                    request.getTitle(),
                    bindingResult.getErrorCount(),
                    bindingResult.getAllErrors());

            return "post/form";
        }

        Long memberId = customUserDetails.getMemberId();
        PostListResponse post = postService.createPost(request, memberId);

        redirectAttributes.addAttribute("id", post.getId());
        redirectAttributes.addFlashAttribute("msg", "게시글이 등록되었습니다.");

        log.info("게시글이 등록 성공 - memberId={}, title={}",
                customUserDetails.getMemberId(),
                request.getTitle());

        return "redirect:/post/{id}";
    }

    @GetMapping("/post/new")
    public String createForm(Model model) {
        model.addAttribute("mode", "create");
        model.addAttribute("form", new PostRequest());
        model.addAttribute("actionUrl", "/post/new");
        model.addAttribute("submitLabel", "등록");
        return "post/form";
    }

    @GetMapping("/post/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model) {

        PostDetailResponse post = postService.getPostDetail(id);

        model.addAttribute("mode", "edit");
        model.addAttribute("form", PostRequest.from(post));
        model.addAttribute("actionUrl", "/post/" + id + "/edit");
        model.addAttribute("submitLabel", "수정");

        return "post/form";
    }

    @PostMapping("/post/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") PostRequest request,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal UnifiedPrincipal user,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.warn("게시글 수정 검증 실패 - memberId={}, title ={}, errorCount={}, errors={}",
                    user != null ? user.getMemberId() : null,
                    request.getTitle(),
                    bindingResult.getErrorCount(),
                    bindingResult.getAllErrors());
            model.addAttribute("mode", "edit");
            model.addAttribute("actionUrl", "/post/" + id + "/edit");
            model.addAttribute("submitLabel", "수정");
            return "post/form";
        }

        postService.update(request, id, user.getMemberId());

        redirectAttributes.addAttribute("id", id);
        redirectAttributes.addFlashAttribute("msg", "게시글 수정 완료");

        log.info("게시글이 수정 성공 - memberId={}, title={}",
                user.getMemberId(),
                request.getTitle());

        return "redirect:/post/{id}";
    }

    @PostMapping("/post/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UnifiedPrincipal user,
                         RedirectAttributes redirectAttributes) {

        postService.delete(id, user.getMemberId());

        redirectAttributes.addFlashAttribute("msg", "게시글이 삭제되었습니다.");

        log.info("게시글 삭제 성공 - memberId={}", user.getMemberId());

        return "redirect:/";
    }
}
