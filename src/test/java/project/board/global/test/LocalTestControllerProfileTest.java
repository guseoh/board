package project.board.global.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class LocalTestControllerProfileTest {

    @Test
    @DisplayName("LocalTestController는 local profile에서만 Bean으로 등록된다")
    void registersOnlyForLocalProfile() {
        assertThat(hasLocalTestController("local")).isTrue();
        assertThat(hasLocalTestController("prod")).isFalse();
        assertThat(hasLocalTestController("test")).isFalse();
    }

    private boolean hasLocalTestController(String profile) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profile);
            context.register(LocalTestController.class);
            context.refresh();
            return context.containsBean("localTestController");
        }
    }
}
