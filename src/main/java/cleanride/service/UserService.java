package cleanride.service;

import cleanride.dto.AuthResponse;
import cleanride.dto.LoginRequest;
import cleanride.dto.PhotoUploadResponse;
import cleanride.dto.RegisterRequest;
import cleanride.dto.UpdatePasswordRequest;
import cleanride.dto.UpdateProfileRequest;
import cleanride.dto.UserProfileResponse;
import cleanride.model.User;
import cleanride.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String username = normalizeUsername(request.getUsername());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("This email is already registered");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("This username is already taken");
        }

        User user = new User();
        
        // Provider-facing accounts are stored as STAFF to match the project blueprint.
        if (isProviderRegistration(request)) {
            user.setName(getRegistrationName(request));
            user.setRole(User.Role.STAFF);
        } else {
            user.setName(getRegistrationName(request));
            user.setRole(User.Role.CUSTOMER);
        }
        
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        return new AuthResponse(savedUser.getId(), toClientRole(savedUser.getRole()), "Registration successful");
    }

    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        return new AuthResponse(user.getId(), toClientRole(user.getRole()), "Login successful");
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return toProfileResponse(user);
    }

    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);
        String normalizedEmail = normalizeEmail(request.getEmail());

        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> { throw new IllegalStateException("Email already in use"); });

        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);

        User savedUser = userRepository.save(user);
        return toProfileResponse(savedUser);
    }

    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = getUserOrThrow(userId);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    public PhotoUploadResponse uploadPhoto(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file is required");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        if (!isSupportedImage(contentType, filename)) {
            throw new IllegalArgumentException("Only .jpg and .png images are allowed");
        }

        User user = getUserOrThrow(userId);
        user.setPhoto(file.getBytes());
        user.setPhotoContentType(file.getContentType());
        userRepository.save(user);

        String reference = "user-" + userId + "-photo";
        return new PhotoUploadResponse(
            "Photo uploaded successfully",
            reference,
            "/api/users/" + userId + "/photo"
        );
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getUserEntity(Long userId) {
        return getUserOrThrow(userId);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        String normalized = username.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (normalized.contains(" ")) {
            throw new IllegalArgumentException("Username cannot contain spaces");
        }
        return normalized;
    }

    private boolean isSupportedImage(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/png")) {
                return true;
            }
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        }
        return false;
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(toClientRole(user.getRole()));
        boolean hasPhoto = user.getPhoto() != null && user.getPhoto().length > 0;
        response.setHasPhoto(hasPhoto);
        if (hasPhoto) {
            response.setPhotoUrl("/api/users/" + user.getId() + "/photo");
        }
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private boolean isProviderRegistration(RegisterRequest request) {
        return "provider".equalsIgnoreCase(request.getRole()) || "staff".equalsIgnoreCase(request.getRole());
    }

    private String getRegistrationName(RegisterRequest request) {
        String value = request.getCarwashName() != null && !request.getCarwashName().trim().isEmpty()
                ? request.getCarwashName()
                : request.getName();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        return value.trim();
    }

    private String toClientRole(User.Role role) {
        return role == User.Role.STAFF ? "STAFF" : role.name();
    }
}
