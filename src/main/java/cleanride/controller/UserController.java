package cleanride.controller;

import cleanride.dto.PhotoUploadResponse;
import cleanride.dto.UpdatePasswordRequest;
import cleanride.dto.UpdateProfileRequest;
import cleanride.dto.UserProfileResponse;
import cleanride.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175"})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        try {
            UserProfileResponse response = userService.getProfile(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
        try {
            UserProfileResponse response = userService.updateProfile(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Long id, @Valid @RequestBody UpdatePasswordRequest request) {
        try {
            userService.updatePassword(id, request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            PhotoUploadResponse response = userService.uploadPhoto(id, file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to read uploaded file"));
        }
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<?> getPhoto(@PathVariable Long id) {
        try {
            var rawUser = userService.getUserEntity(id);
            if (rawUser.getPhoto() == null || rawUser.getPhoto().length == 0) {
                return ResponseEntity.notFound().build();
            }
            var contentType = rawUser.getPhotoContentType();
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().contentType(Objects.requireNonNull(mediaType)).body(rawUser.getPhoto());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }
}
