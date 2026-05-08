package project.board.global.security.config.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.board.global.security.user.UnifiedPrincipal;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional //todo: ??
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuthUserInfo oAuthUserInfo = null;


        //todo: switch 방식 개선
        if ("google".equals(provider)) {
            oAuthUserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if ("naver".equals(provider)) {
            oAuthUserInfo = new NaverUserInfo((Map<String, Object>) oAuth2User.getAttributes().get("response"));
        } else if ("kakao".equals(provider)){
            oAuthUserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        }
        else {
            throw new OAuth2AuthenticationException("지원하지 않는 로그인 유형입니다.");
        }


        String providerId = oAuthUserInfo.getProviderId();
        String email = oAuthUserInfo.getEmail();
        String name = oAuthUserInfo.getName();

        Member findMember = memberRepository.findByProviderAndProviderId(provider, providerId).orElseGet(() -> {

//            String nickname = email.substring(0, email.indexOf("@"));

            String dummy = UUID.randomUUID().toString();
            String encode = passwordEncoder.encode(dummy);

            Member member = Member.createOAuth(
                    name,
                    email,
                    encode,
                    Role.USER,
                    provider,
                    providerId);
            return memberRepository.save(member);
        });

        // 기존 회원인데 provider 정보 없는 경우
        //findMember.updateOAuth(provider, providerId);

        return new UnifiedPrincipal(
                findMember.getNickname(),
                findMember.getId(),
                findMember.getEmail(),
                findMember.getRole().getKey(),
                findMember.getPassword(),
                findMember.getProvider(),
                findMember.getProviderId(),
                attributes
        );
    }

}

