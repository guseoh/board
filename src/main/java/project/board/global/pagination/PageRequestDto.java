package project.board.global.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 5;

    @Builder.Default
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    private String keyword;

    public Pageable getPageable(Sort sort) {
        return PageRequest.of(page - 1, size, sort);
    }


    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
