package project.board.global.dto;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter     // PageRequestDto 바인딩이 안 돼서 size=0으로 남는 경우
public class PageRequestDto {

    @Builder.Default
    private int size = 5;

    @Builder.Default
    private int page = 1;

    private String keyword;

    public Pageable getPageable(Sort sort) {
        return PageRequest.of(page - 1, size, sort);
    }


    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
