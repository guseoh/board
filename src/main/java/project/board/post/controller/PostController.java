package project.board.post.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.post.dto.PostDto;
import project.board.post.service.PostService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
@Slf4j
public class PostController {

    private final PostService postService;

    // 전체 조회
    @GetMapping()
    public String list(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<PostDto.Response> page = postService.findAll(pageable);

        model.addAttribute("page", page);
        model.addAttribute("posts", page.getContent());

        return "post/list";
    }

    // 단일 조회
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PostDto.Response post = postService.findOne(id);

        model.addAttribute("post", post);

        return "post/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new PostDto.CreateRequest());

        return "post/create";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("request") PostDto.CreateRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "post/create";
        }

        Long memberId = 1L; // 임시 -> Security에서 추출

        PostDto.Response post = postService.save(request, memberId);
        redirectAttributes.addAttribute("id", post.getId());
        redirectAttributes.addFlashAttribute("message", "게시글이 등록되었습니다.");

        return "redirect:/post/{id}";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PostDto.Response post = postService.findOne(id);

        // todo: 정리
        PostDto.UpdateRequest request = new PostDto.UpdateRequest();
        request.setTitle(post.getTitle());
        request.setContent(post.getContent());
        
        model.addAttribute("postId", id);
        model.addAttribute("request", request);
        model.addAttribute("post", post);

        return "post/edit";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("request") PostDto.UpdateRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            //todo: 에러 시 기존 게시글 정보 추가
            return "post/edit";
        }

        postService.update(request, id);
        redirectAttributes.addAttribute("id", id);
        redirectAttributes.addFlashAttribute("message", "게시글 수정 완료");

        return "redirect:/post/{id}";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        postService.delete(id);

        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");

        return "redirect:/post";
    }
}
