package project.board.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.board.member.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String username);

    boolean existsByEmail(String email);

    //todo: 정리
    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

}
