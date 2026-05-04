package cleanride.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer duration;
    private Long providerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceDTO() {}

    public ServiceDTO(Long id, String name, String description, BigDecimal price, 
                     Integer duration, Long providerId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.duration = duration;
        this.providerId = providerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
