package project.board.post.dto.response;

import project.board.global.pagination.PageResultDto;

import java.util.List;

public record PostPageApiResponse(
        List<PostListResponse> content,
        int page,
        int size,
        int totalPages,
        long totalElements,
        boolean first,
        boolean last
) {
    public PostPageApiResponse {
        content = content == null
                ? List.of()
                : List.copyOf(content);
    }

    public static PostPageApiResponse from(
            PageResultDto<PostListResponse, ?> pageResult
    ) {
        int currentPage = pageResult.getPage();
        int totalPages = pageResult.getTotalPage();

        boolean first = totalPages == 0 || currentPage == 1;
        boolean last = totalPages == 0 || currentPage == totalPages;

        return new PostPageApiResponse(
                pageResult.getDtoList(),
                currentPage,
                pageResult.getSize(),
                totalPages,
                pageResult.getTotalCount(),
                first,
                last
        );
    }
}


//todo: 방어 코드
//todo: PageResponse dto를 사용하고 from 메서드를 별도로 만드는 이유
//todo: pageDto를 사용하지 않는 방향 -> 김영한 강의 JPA 활용 2편