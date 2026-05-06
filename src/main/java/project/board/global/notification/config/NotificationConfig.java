package project.board.global.notification.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import project.board.global.notification.discord.DiscordWebhookProperties;

@Configuration
@EnableConfigurationProperties(DiscordWebhookProperties.class)
public class NotificationConfig {
}
