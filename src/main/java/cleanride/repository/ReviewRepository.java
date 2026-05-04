package cleanride.repository;

import cleanride.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByServiceId(Long serviceId);
    List<Review> findByUserId(Long userId);
    Optional<Review> findByBookingId(Long bookingId);
}
