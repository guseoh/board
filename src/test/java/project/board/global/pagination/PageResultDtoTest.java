package project.board.global.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultDtoTest {

    @Test
    @DisplayName("검색 결과가 없으면 빈 목록과 빈 페이지 블록을 반환한다")
    void emptyPage() {
        PageResultDto<String, String> result = new PageResultDto<>(
                new PageImpl<>(List.of(), PageRequest.of(0, 5), 0)
        );

        assertThat(result.getDtoList()).isEmpty();
        assertThat(result.getTotalPage()).isZero();
        assertThat(result.getPageList()).isEmpty();
        assertThat(result.getStart()).isZero();
        assertThat(result.getEnd()).isZero();
        assertThat(result.isPrev()).isFalse();
        assertThat(result.isNext()).isFalse();
    }

    @Test
    @DisplayName("전체 페이지 수가 블록보다 작으면 첫 페이지부터 마지막 페이지만 표시한다")
    void pageBlockSmallerThanTen() {
        PageResultDto<String, String> result = new PageResultDto<>(
                new PageImpl<>(List.of("a"), PageRequest.of(0, 5), 15)
        );

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageList()).containsExactly(1, 2, 3);
        assertThat(result.isPrev()).isFalse();
        assertThat(result.isNext()).isFalse();
    }

    @Test
    @DisplayName("첫 페이지와 마지막 페이지에서 이전·다음 블록을 올바르게 계산한다")
    void firstAndLastPageBlocks() {
        PageResultDto<String, String> first = new PageResultDto<>(
                new PageImpl<>(List.of("first"), PageRequest.of(0, 1), 25)
        );
        PageResultDto<String, String> last = new PageResultDto<>(
                new PageImpl<>(List.of("last"), PageRequest.of(24, 1), 25)
        );

        assertThat(first.getPageList()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(first.isPrev()).isFalse();
        assertThat(first.isNext()).isTrue();
        assertThat(last.getPageList()).containsExactly(21, 22, 23, 24, 25);
        assertThat(last.isPrev()).isTrue();
        assertThat(last.isNext()).isFalse();
    }
}
