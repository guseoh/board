package project.board.global.notification.discord;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "discord.webhook")
public class DiscordWebhookProperties {
    private String url;
    private boolean enabled;
}
