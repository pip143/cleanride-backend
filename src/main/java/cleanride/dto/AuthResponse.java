package cleanride.dto;

public class AuthResponse {

    private Long userId;
    private String role;
    private String message;

    public AuthResponse() {}

    public AuthResponse(Long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public AuthResponse(Long userId, String role, String message) {
        this.userId = userId;
        this.role = role;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
