package project.board.global.security.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import project.board.global.security.principal.UnifiedPrincipal;
import project.board.member.entity.LoginType;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Member member = memberRepository.findByEmailAndLoginType(username, LoginType.LOCAL)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));

        return UnifiedPrincipal.from(member);
    }
}
