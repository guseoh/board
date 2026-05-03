package project.board.global.notification.discord;

public record DiscordMessage(
        String content,
        String username
) {
    public static DiscordMessage of(String content) {
        return new DiscordMessage(content, "Board 알림봇");
    }
}
