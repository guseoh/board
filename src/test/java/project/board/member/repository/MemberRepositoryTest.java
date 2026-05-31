package project.board.member.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import project.board.global.security.config.JpaConfig;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("finds local member by email and checks duplicates")
    void findByEmailAndExists() {
        Member saved = memberRepository.save(Member.create(
                "tester",
                "tester@example.com",
                "encoded",
                Role.USER,
                LoginType.LOCAL
        ));

        assertThat(memberRepository.findByEmail("tester@example.com")).contains(saved);
        assertThat(memberRepository.existsByEmail("tester@example.com")).isTrue();
        assertThat(memberRepository.existsByNickname("tester")).isTrue();
        assertThat(memberRepository.existsByNicknameAndIdNot("tester", saved.getId())).isFalse();
        assertThat(memberRepository.existsByNicknameAndIdNot("tester", saved.getId() + 100)).isTrue();
    }

    @Test
    @DisplayName("finds OAuth member by provider and providerId")
    void findByProviderAndProviderId() {
        Member saved = memberRepository.save(Member.createOAuth(
                "google-user",
                "google@example.com",
                "dummy",
                Role.USER,
                "google",
                "google-123",
                LoginType.SOCIAL
        ));

        assertThat(memberRepository.findByProviderAndProviderId("google", "google-123")).contains(saved);
        assertThat(memberRepository.findByProviderAndProviderId("kakao", "google-123")).isEmpty();
    }
}
