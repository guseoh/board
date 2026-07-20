package project.board.global.pagination;

import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
public class PageResultDto<DTO, EN> {

    private List<DTO> dtoList;  // 실제 목록 데이터

    private int totalPage;
    private long totalCount;

    private int start, end;

    private int page;   // 현재 페이지 번호
    private int size;   // 한 페이지에 보여줄 데이터 개수

    private boolean prev, next;

    private List<Integer> pageList; // 화면에 출력할 페이지 번호

    // Function<EN, DTO> fn: 엔티티를 DTO로 변환
    public PageResultDto(Page<EN> result, Function<EN, DTO> fn) {
        dtoList = result.stream().map(fn).collect(Collectors.toList()); // to.List() 차이점

        totalPage = result.getTotalPages();
        totalCount = result.getTotalElements();

        makePageList(result.getPageable());
    }

    public PageResultDto(Page<DTO> result) {
        dtoList = result.getContent();

        totalPage = result.getTotalPages();
        totalCount = result.getTotalElements();

        makePageList(result.getPageable());
    }

    private void makePageList(Pageable pageable) {

        this.page = pageable.getPageNumber() + 1;
        this.size = pageable.getPageSize();

        if (totalPage == 0) {
            start = 0;
            end = 0;
            prev = false;
            next = false;
            pageList = List.of();
            return;
        }

        int tempEnd = (int) ((Math.ceil(page / 10.0)) * 10);
        start = tempEnd - 9;

        prev = start > 1;
        end = Math.min(totalPage, tempEnd);
        next = totalPage > tempEnd;

        pageList = IntStream.rangeClosed(start, end).boxed().toList();
    }
}
