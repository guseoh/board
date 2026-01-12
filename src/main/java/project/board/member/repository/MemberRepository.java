package project.board.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.board.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

}
