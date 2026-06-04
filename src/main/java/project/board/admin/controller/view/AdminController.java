package project.board.admin.controller.view;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import project.board.member.entity.Member;
import project.board.member.service.MemberService;
import project.board.post.entity.Post;
import project.board.post.service.PostService;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PostService postService;
    private final MemberService memberService;

    @GetMapping
    public String index(Model model) {
        long totalPosts = postService.count();
        long totalUsers = memberService.countMember();

        model.addAttribute("totalPosts", totalPosts);
        model.addAttribute("totalUsers", totalUsers);

        return "admin/index";
    }

    @GetMapping("/posts")
    public String posts(Model model) {
        List<Post> posts = postService.findAllAdmin();

        model.addAttribute("posts", posts);

        return "admin/posts";
    }

    @PostMapping("/posts/{postId}/delete")
    public String deletePosts(@PathVariable Long postId) {
        postService.deleteForAdmin(postId);

        return "redirect:/admin";
    }

    @GetMapping("/users")
    public String members(Model model) {
        List<Member> members = memberService.findAllForAdmin();
        model.addAttribute("members", members);

        return "admin/users";
    }

    @PostMapping("/users/{memberId}/role")
    public String memberUpdate(@PathVariable Long memberId,
                               @RequestParam String role) {
        memberService.roleChange(role, memberId);

        return "redirect:/admin";
    }

    @PostMapping("/users/{memberId}/delete")
    public String memberDelete(@PathVariable Long memberId) {
        memberService.deleteForAdmin(memberId);

        return "redirect:/admin";
    }
}
