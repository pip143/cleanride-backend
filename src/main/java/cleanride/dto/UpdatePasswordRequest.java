package cleanride.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdatePasswordRequest {

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
