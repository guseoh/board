package project.board.global.api.error;

public record ApiFieldError(
        String field,
        String message
) {
}
