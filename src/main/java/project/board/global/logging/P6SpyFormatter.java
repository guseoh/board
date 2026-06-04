package project.board.global.logging;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P6SpyFormatter implements MessageFormattingStrategy {

    private static final Pattern VALUE_PATTERN =
            Pattern.compile("'(?:''|[^'])*'|\\btrue\\b|\\bfalse\\b|-?\\d+(?:\\.\\d+)?|null",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared,
                                String sql, String url) {

        if (sql == null || sql.isBlank()) {
            return "";
        }

        String caller = findCaller();
        String formattedSql = formatSql(sql);
        String params = extractParams(prepared, sql);

        if ("[]".equals(params)) {
            return String.format(
                    "%n[CALLER] %s%n[SQL] %dms%n%s%n",
                    caller,
                    elapsed,
                    formattedSql
            );
        }

        return String.format(
                "%n[CALLER] %s%n[SQL] %dms%n[PARAMS] %s%n%s%n",
                caller,
                elapsed,
                params,
                formattedSql
        );
    }

    private String findCaller() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            String lowerClassName = className.toLowerCase();

            boolean isProjectClass = className.startsWith("project.board");
            boolean isTargetLayer =
                    lowerClassName.contains(".controller.")
                            || lowerClassName.contains(".service.")
                            || lowerClassName.contains(".repository.");

            boolean isExcludeClass =
                    className.contains("P6SpyFormatter")
                            || className.startsWith("com.p6spy")
                            || className.startsWith("java.")
                            || className.startsWith("jdk.")
                            || className.startsWith("sun.")
                            || className.startsWith("org.hibernate")
                            || className.startsWith("org.springframework");

            if (isProjectClass && isTargetLayer && !isExcludeClass) {
                return className + "." + element.getMethodName()
                        + "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
            }
        }

        return "Unknown";
    }

    private String extractParams(String prepared, String sql) {
        if (prepared == null || prepared.isBlank() || sql == null || sql.isBlank()) {
            return "[]";
        }

        long questionCount = prepared.chars().filter(ch -> ch == '?').count();
        if (questionCount == 0) {
            return "[]";
        }

        List<String> values = new ArrayList<>();
        Matcher matcher = VALUE_PATTERN.matcher(sql);

        while (matcher.find()) {
            values.add(matcher.group());
        }

        if (values.isEmpty()) {
            return "[]";
        }

        int startIndex = Math.max(0, values.size() - (int) questionCount);
        List<String> params = values.subList(startIndex, values.size());

        return params.toString();
    }

    private String formatSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return "";
        }

        String normalized = sql
                .replaceAll("\\s+", " ")
                .trim();

        return normalized
                .replaceAll("(?i)\\bselect\\b", "select")
                .replaceAll("(?i)\\bfrom\\b", "\nfrom")
                .replaceAll("(?i)\\bleft join\\b", "\nleft join")
                .replaceAll("(?i)\\bright join\\b", "\nright join")
                .replaceAll("(?i)\\binner join\\b", "\ninner join")
                .replaceAll("(?i)\\bjoin\\b", "\njoin")
                .replaceAll("(?i)\\bon\\b", "\n    on")
                .replaceAll("(?i)\\bwhere\\b", "\nwhere")
                .replaceAll("(?i)\\band\\b", "\n    and")
                .replaceAll("(?i)\\bor\\b", "\n    or")
                .replaceAll("(?i)\\bgroup by\\b", "\ngroup by")
                .replaceAll("(?i)\\border by\\b", "\norder by")
                .replaceAll("(?i)\\bhaving\\b", "\nhaving")
                .replaceAll("(?i)\\blimit\\b", "\nlimit")
                .replaceAll("(?i)\\bvalues\\b", "\nvalues")
                .replaceAll("(?i)\\bset\\b", "\nset")
                .replaceFirst("(?i)^select\\s+", "select\n    ")
                .replaceAll(",\\s*", ",\n    ")
                .trim();
    }
}