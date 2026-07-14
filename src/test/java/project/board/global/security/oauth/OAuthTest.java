package project.board.global.security.oauth;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import project.board.global.security.principal.UnifiedPrincipal;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static project.board.testsupport.TestFixtures.oauthMember;
import static project.board.testsupport.TestFixtures.setId;

class OAuthTest {

    @Test
    @DisplayName("구글, 네이버, 카카오 사용자 정보를 파싱한다")
    void parsesProviderUserInfo() {
        GoogleUserInfo google = new GoogleUserInfo(Map.of(
                "sub", "google-1",
                "email", "google@example.com",
                "name", "Google User"
        ));
        NaverUserInfo naver = new NaverUserInfo(Map.of(
                "id", "naver-1",
                "email", "naver@example.com",
                "name", "Naver User"
        ));
        KakaoUserInfo kakao = new KakaoUserInfo(Map.of(
                "id", 12345,
                "kakao_account", Map.of(
                        "email", "kakao@example.com",
                        "profile", Map.of("nickname", "Kakao User")
                )
        ));

        assertThat(google.getProvider()).isEqualTo("google");
        assertThat(google.getProviderId()).isEqualTo("google-1");
        assertThat(google.getEmail()).isEqualTo("google@example.com");
        assertThat(google.getName()).isEqualTo("Google User");

        assertThat(naver.getProvider()).isEqualTo("naver");
        assertThat(naver.getProviderId()).isEqualTo("naver-1");
        assertThat(naver.getEmail()).isEqualTo("naver@example.com");
        assertThat(naver.getName()).isEqualTo("Naver User");

        assertThat(kakao.getProvider()).isEqualTo("kakao");
        assertThat(kakao.getProviderId()).isEqualTo("12345");
        assertThat(kakao.getEmail()).isEqualTo("kakao@example.com");
        assertThat(kakao.getName()).isEqualTo("Kakao User");
    }

    @Test
    @DisplayName("카카오 사용자 정보에 이메일이 없으면 대체 값을 사용한다")
    void kakaoMissingEmailFallback() {
        KakaoUserInfo kakao = new KakaoUserInfo(Map.of(
                "id", 12345,
                "kakao_account", Map.of("profile", Map.of("nickname", "Kakao User"))
        ));

        assertThat(kakao.getEmail()).isEqualTo("kakao_12345@oauth.local");
        assertThat(kakao.getName()).isEqualTo("Kakao User");
    }

    @Test
    @DisplayName("커스텀 소셜 로그인 서비스는 새 소셜 로그인 회원을 생성한다")
    void createsNewOauthMember() throws Exception {
        withUserInfoServer(
                "{\"sub\":\"google-1\",\"email\":\"google@example.com\",\"name\":\"Google User\"}",
                userInfoUri -> {
                    MemberRepository memberRepository = mock(MemberRepository.class);
                    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
                    CustomOauth2UserService service = new CustomOauth2UserService(memberRepository, passwordEncoder);
                    given(memberRepository.findByProviderAndProviderId("google", "google-1")).willReturn(Optional.empty());
                    given(passwordEncoder.encode(any())).willReturn("encoded-dummy");
                    given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
                        Member saved = invocation.getArgument(0);
                        setId(saved, 1L);
                        return saved;
                    });

                    OAuth2User user = service.loadUser(userRequest("google", "sub", userInfoUri));

                    assertThat(user).isInstanceOf(UnifiedPrincipal.class);
                    UnifiedPrincipal principal = (UnifiedPrincipal) user;
                    assertThat(principal.getMemberId()).isEqualTo(1L);
                    assertThat(principal.getProvider()).isEqualTo("google");
                    assertThat(principal.getProviderId()).isEqualTo("google-1");
                    assertThat(principal.getLoginType()).isEqualTo(LoginType.SOCIAL);
                    verify(memberRepository).save(any(Member.class));
                }
        );
    }

    @Test
    @DisplayName("커스텀 소셜 로그인 서비스는 기존 소셜 로그인 회원을 재사용한다")
    void reusesExistingOauthMember() throws Exception {
        withUserInfoServer(
                "{\"sub\":\"google-1\",\"email\":\"google@example.com\",\"name\":\"Google User\"}",
                userInfoUri -> {
                    MemberRepository memberRepository = mock(MemberRepository.class);
                    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
                    CustomOauth2UserService service = new CustomOauth2UserService(memberRepository, passwordEncoder);
                    Member existing = oauthMember(7L, "google", "google-1");
                    given(memberRepository.findByProviderAndProviderId("google", "google-1")).willReturn(Optional.of(existing));

                    OAuth2User user = service.loadUser(userRequest("google", "sub", userInfoUri));

                    UnifiedPrincipal principal = (UnifiedPrincipal) user;
                    assertThat(principal.getMemberId()).isEqualTo(7L);
                    assertThat(principal.getProvider()).isEqualTo("google");
                    assertThat(principal.getProviderId()).isEqualTo("google-1");
                    verify(memberRepository, never()).save(any(Member.class));
                }
        );
    }

    @Test
    @DisplayName("커스텀 소셜 로그인 서비스는 응답 객체가 없는 네이버 응답을 거부한다")
    void rejectsMissingNaverResponse() throws Exception {
        withUserInfoServer(
                "{\"id\":\"naver-1\",\"email\":\"naver@example.com\",\"name\":\"Naver User\"}",
                userInfoUri -> {
                    MemberRepository memberRepository = mock(MemberRepository.class);
                    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
                    CustomOauth2UserService service = new CustomOauth2UserService(memberRepository, passwordEncoder);

                    assertThatThrownBy(() -> service.loadUser(userRequest("naver", "id", userInfoUri)))
                            .isInstanceOf(OAuth2AuthenticationException.class);
                }
        );
    }

    private OAuth2UserRequest userRequest(String registrationId, String userNameAttribute, String userInfoUri) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("http://localhost/oauth2/authorize")
                .tokenUri("http://localhost/oauth2/token")
                .userInfoUri(userInfoUri)
                .userNameAttributeName(userNameAttribute)
                .clientName(registrationId)
                .build();
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(60)
        );
        return new OAuth2UserRequest(registration, token);
    }

    private void withUserInfoServer(String body, UserInfoCallback callback) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/userinfo", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            callback.execute("http://localhost:" + server.getAddress().getPort() + "/userinfo");
        } finally {
            server.stop(0);
        }
    }

    @FunctionalInterface
    private interface UserInfoCallback {
        void execute(String userInfoUri) throws IOException;
    }
}
