package project.board.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.board.comment.entity.Comment;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.post.entity.Post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityDomainTest {

    @Test
    @DisplayName("Member supports local and OAuth creation and mutable profile fields")
    void memberCreateAndChange() {
        Member local = Member.create("local", "local@example.com", "encoded", Role.USER, LoginType.LOCAL);
        Member oauth = Member.createOAuth(
                "oauth",
                "oauth@example.com",
                "dummy",
                Role.USER,
                "google",
                "google-1",
                LoginType.SOCIAL
        );

        local.changeNickname("changed");
        local.changePassword("new-encoded");
        local.changeRole(Role.ADMIN);

        assertThat(local.getNickname()).isEqualTo("changed");
        assertThat(local.getPassword()).isEqualTo("new-encoded");
        assertThat(local.getRole()).isEqualTo(Role.ADMIN);
        assertThat(local.getLoginType()).isEqualTo(LoginType.LOCAL);
        assertThat(oauth.getProvider()).isEqualTo("google");
        assertThat(oauth.getProviderId()).isEqualTo("google-1");
        assertThat(oauth.getLoginType()).isEqualTo(LoginType.SOCIAL);
    }

    @Test
    @DisplayName("Post creation validates required fields and connects writer")
    void postCreateValidatesAndAssignsMember() {
        Member writer = Member.create("writer", "writer@example.com", "encoded", Role.USER, LoginType.LOCAL);

        Post post = Post.create("title", "content", writer);

        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getContent()).isEqualTo("content");
        assertThat(post.getMember()).isSameAs(writer);
        assertThat(writer.getPosts()).contains(post);

        assertThatThrownBy(() -> Post.create("", "content", writer))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_TITLE.getMessage());
        assertThatThrownBy(() -> Post.create("title", " ", writer))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_CONTENT.getMessage());
        assertThatThrownBy(() -> Post.create("title", "content", null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_MEMBER.getMessage());
    }

    @Test
    @DisplayName("Post change validates title and content")
    void postChangeValidatesContent() {
        Member writer = Member.create("writer", "writer@example.com", "encoded", Role.USER, LoginType.LOCAL);
        Post post = Post.create("old title", "old content", writer);

        post.change("new title", "new content");

        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
        assertThatThrownBy(() -> post.change("", "content"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_TITLE.getMessage());
        assertThatThrownBy(() -> post.change("title", ""))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_CONTENT.getMessage());
    }

    @Test
    @DisplayName("Comment creation connects post, member, parent and children")
    void commentCreateReplyAndChange() {
        Member writer = Member.create("writer", "writer@example.com", "encoded", Role.USER, LoginType.LOCAL);
        Member commenter = Member.create("commenter", "commenter@example.com", "encoded", Role.USER, LoginType.LOCAL);
        Post post = Post.create("title", "content", writer);

        Comment root = Comment.create("comment", commenter, post, null);
        Comment reply = Comment.create("reply", writer, post, root);

        root.changeContent("changed comment");

        assertThat(root.getContent()).isEqualTo("changed comment");
        assertThat(root.getMember()).isSameAs(commenter);
        assertThat(root.getPost()).isSameAs(post);
        assertThat(root.rootComment()).isTrue();
        assertThat(reply.isReply()).isTrue();
        assertThat(reply.getParent()).isSameAs(root);
        assertThat(root.getChildren()).contains(reply);
        assertThat(post.getComments()).contains(root, reply);
    }
}
