package project.board.member.entity;

public enum Role {
    ADMIN("ROLE_ADMIN"), USER("ROLE_USER");

    private final String key;

    Role(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
