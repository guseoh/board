package project.board.global.security.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Member member = memberRepository.findByEmail(username).orElseThrow(() ->
                new IllegalArgumentException("User not found: " + username));

        return new CustomUserDetails(
                member.getNickname(),
                member.getPassword(),
                member.getId(),
                member.getEmail(),
                member.getRole().getKey()
        );
    }
}
