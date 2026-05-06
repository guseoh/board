package project.board.post.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;

@Profile("local")
@RestController
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/test/discord-error")
    public String discordErrorTest() {
        throw new CustomException(ErrorCode.POST_NOT_FOUND);
    }
}
