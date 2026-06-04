package project.board.global.security.oauth;

/*
    공통 정보를 저장할 인터페이스
 */
public interface OAuthUserInfo {

    String getProvider();

    String getProviderId();

    String getEmail();

    String getName();
}
