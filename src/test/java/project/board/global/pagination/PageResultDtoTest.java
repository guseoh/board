package project.board.global.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultDtoTest {

    @Test
    @DisplayName("빈 페이지는 페이지 번호 목록과 이동 상태를 비운다")
    void emptyPageContract() {
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);

        PageResultDto<String, String> result = new PageResultDto<>(page);

        assertThat(result.getDtoList()).isEmpty();
        assertThat(result.getTotalPage()).isZero();
        assertThat(result.getTotalCount()).isZero();
        assertThat(result.getStart()).isZero();
        assertThat(result.getEnd()).isZero();
        assertThat(result.isPrev()).isFalse();
        assertThat(result.isNext()).isFalse();
        assertThat(result.getPageList()).isEmpty();
    }

    @Test
    @DisplayName("10페이지 단위의 페이지 블록 경계를 계산한다")
    void pageBlockContract() {
        assertPageBlock(9, 10, 1, 10, false, true);
        assertPageBlock(10, 11, 11, 20, true, true);
        assertPageBlock(20, 21, 21, 21, true, false);
    }

    private void assertPageBlock(int pageIndex,
                                 int expectedPage,
                                 int expectedStart,
                                 int expectedEnd,
                                 boolean expectedPrev,
                                 boolean expectedNext) {
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(pageIndex, 5), 105);

        PageResultDto<String, String> result = new PageResultDto<>(page);

        assertThat(result.getPage()).isEqualTo(expectedPage);
        assertThat(result.getStart()).isEqualTo(expectedStart);
        assertThat(result.getEnd()).isEqualTo(expectedEnd);
        assertThat(result.isPrev()).isEqualTo(expectedPrev);
        assertThat(result.isNext()).isEqualTo(expectedNext);
        assertThat(result.getPageList()).containsExactlyElementsOf(
                IntStream.rangeClosed(expectedStart, expectedEnd).boxed().toList()
        );
    }
}
