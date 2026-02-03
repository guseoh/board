package project.board.post.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.global.security.user.CustomUserDetails;
import project.board.post.dto.PostDto;
import project.board.post.service.PostService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;

    // 전체 조회
    @GetMapping({"", "/"})
    public String list(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<PostDto.Response> page = postService.findAll(pageable);

        model.addAttribute("page", page);
        model.addAttribute("posts", page.getContent());

        return "post/list";
    }

    // 단일 조회
    @GetMapping("/post/{id}")
    public String detail(@PathVariable Long id, Model model) {

        //todo: service 두 번 호출
        postService.viewCount(id);
        PostDto.Response post = postService.findOne(id);

        model.addAttribute("post", post);

        return "post/detail";
    }

    //todo: 작성도 인증된 사람만
    @PostMapping("/post/new")
    public String create(@Valid @ModelAttribute("form") PostDto.CreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails customUserDetails,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "post/form";
        }

        Long memberId = customUserDetails.getMemberId();
        PostDto.Response post = postService.save(request, memberId);
        redirectAttributes.addAttribute("id", post.getId());
        redirectAttributes.addFlashAttribute("msg", "게시글이 등록되었습니다.");
        log.info("게시글이 등록되었습니다.");

        return "redirect:/post/{id}";
    }

    @GetMapping("/post/new")
    public String createForm(Model model) {
        model.addAttribute("mode", "create");
        model.addAttribute("form", new PostDto.CreateRequest());
        model.addAttribute("actionUrl", "/post/new");
        model.addAttribute("submitLabel", "등록");
        return "post/form";
    }

    @GetMapping("/post/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails user,
                           Model model) {

        PostDto.Response post = postService.findOne(id);

        PostDto.UpdateRequest form = new PostDto.UpdateRequest();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());

        model.addAttribute("mode", "edit");
        model.addAttribute("form", form);
        model.addAttribute("actionUrl", "/post/" + id + "/edit");
        model.addAttribute("submitLabel", "수정");

        return "post/form";
    }

    @PostMapping("/post/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") PostDto.UpdateRequest form,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal CustomUserDetails user,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("actionUrl", "/post/" + id + "/edit");
            model.addAttribute("submitLabel", "수정");
            return "post/form";
        }

        log.info("게시글 수정이 완료되었습니다.");

        postService.update(form, id, user.getMemberId());

        redirectAttributes.addAttribute("id", id);
        redirectAttributes.addFlashAttribute("msg", "게시글 수정 완료");

        return "redirect:/post/{id}";
    }

    @PostMapping("/post/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails user,
                         RedirectAttributes redirectAttributes) {

        postService.delete(id, user.getMemberId());

        redirectAttributes.addFlashAttribute("msg", "게시글이 삭제되었습니다.");

        return "redirect:/";
    }
}
