package project.board.global.security.config.oauth;

import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class KakaoUserInfo implements OAuthUserInfo{

    private Map<String, Object> attributes;

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        if (kakaoAccount == null || kakaoAccount.get("email") == null) {
            return getProvider() + "_" + getProviderId() + "oauth.lcoal";
        }

        return kakaoAccount.get("email").toString();
    }

    @Override
    public String getName() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        Map<String, Object> nickname = (Map<String, Object>) kakaoAccount.get("profile");

        return nickname.get("nickname").toString();
    }
}
