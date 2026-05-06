package project.board.global.notification.discord;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordNotifier {
    private final DiscordWebhookProperties properties;
    private final RestClient.Builder restBuilder;

    public void send(String message) {
        if (!properties.isEnabled()) {
            return;
        }

        if (!StringUtils.hasText(properties.getUrl())) {
            log.warn("Discord URL is empty");
            return;
        }

        try {
            RestClient restClient = restBuilder.build();

            restClient.post()
                    .uri(properties.getUrl())
                    .body(DiscordMessage.of(message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Discord 알림 전송 실패: {}", e.getMessage());
        }
    }
}
