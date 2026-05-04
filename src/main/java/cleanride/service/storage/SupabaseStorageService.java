package cleanride.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Objects;

@Service
public class SupabaseStorageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String supabaseUrl;
    private final String supabaseSecretKey;
    private final String bucket;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.secret-key:}") String supabaseSecretKey,
            @Value("${supabase.bucket:}") String bucket) {
        this.supabaseUrl = trimTrailingSlash(supabaseUrl);
        this.supabaseSecretKey = supabaseSecretKey;
        this.bucket = bucket;
    }

    public UploadResult uploadProfilePhoto(Long userId, MultipartFile file) throws IOException {
        validateConfig();
        String extension = resolveExtension(file);
        String fileName = "profile." + extension;
        String path = "users/" + userId + "/" + fileName;

        String uploadUrl = UriComponentsBuilder.fromHttpUrl(Objects.requireNonNull(supabaseUrl))
                .pathSegment("storage", "v1", "object")
                .pathSegment(bucket)
                .pathSegment("users", String.valueOf(userId), fileName)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(Objects.requireNonNull(supabaseSecretKey));
        headers.set("apikey", supabaseSecretKey);
        headers.setContentType(resolveContentType(file));
        headers.set("x-upsert", "true");

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        restTemplate.exchange(uploadUrl, Objects.requireNonNull(HttpMethod.POST), entity, String.class);

        return new UploadResult(path, buildPublicUrl(path));
    }

    public String buildPublicUrl(String path) {
        validateConfig();
        return UriComponentsBuilder.fromHttpUrl(Objects.requireNonNull(supabaseUrl))
                .pathSegment("storage", "v1", "object", "public")
                .pathSegment(bucket)
                .path(path.startsWith("/") ? path : "/" + path)
                .build()
                .toUriString();
    }

    private void validateConfig() {
        if (!StringUtils.hasText(supabaseUrl)) {
            throw new IllegalStateException("SUPABASE_URL is not set");
        }
        if (!StringUtils.hasText(supabaseSecretKey)) {
            throw new IllegalStateException("SUPABASE_SECRET_KEY is not set");
        }
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("SUPABASE_BUCKET is not set");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (StringUtils.hasText(filename) && Objects.requireNonNull(filename).contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.equalsIgnoreCase("image/png")) {
            return "png";
        }
        return "jpg";
    }

    private MediaType resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            return MediaType.parseMediaType(contentType);
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record UploadResult(String path, String publicUrl) {}
}
