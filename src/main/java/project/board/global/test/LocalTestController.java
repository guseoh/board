package project.board.global.test;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;

@Profile("local")
@RestController
@RequiredArgsConstructor
public class LocalTestController {

    @GetMapping("/test/discord-error")
    public String discordErrorTest() {
        throw new CustomException(ErrorCode.POST_NOT_FOUND);
    }
}
