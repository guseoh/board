package project.board.global.security.config.oauth;

public interface OAuthUserInfo {

    String getProvider();

    String getProviderId();

    String getEmail();

    String getName();
}
