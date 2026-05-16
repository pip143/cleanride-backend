package cleanride.controller;

import cleanride.model.Review;
import cleanride.repository.BookingRepository;
import cleanride.repository.ReviewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://*.vercel.app"})
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public ReviewController(ReviewRepository reviewRepository, BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/service/{serviceId}")
    public List<Map<String, Object>> getReviewsForService(@PathVariable Long serviceId) {
        return reviewRepository.findByServiceId(serviceId)
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> getReviewsForUser(@PathVariable Long userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getReviewForBooking(@PathVariable Long bookingId) {
        var review = reviewRepository.findByBookingId(bookingId);
        if (review.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toReviewResponse(review.get()));
    }

    @PostMapping
    public ResponseEntity<?> addReviewFromBody(@RequestBody Map<String, Object> reviewData) {
        Long bookingId = getLongValue(reviewData.get("bookingId"));
        if (bookingId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "bookingId is required"));
        }

        Review review = new Review();
        review.setRating(getIntegerValue(reviewData.get("rating")));
        review.setComment(getStringValue(reviewData.get("comment")));
        return saveReviewForBooking(bookingId, review);
    }

    @PostMapping("/{bookingId}")
    public ResponseEntity<?> addReview(@PathVariable Long bookingId, @RequestBody Review review) {
        return saveReviewForBooking(bookingId, review);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(@PathVariable Long reviewId, @RequestBody Review reviewDetails) {
        var reviewOptional = reviewRepository.findById(Objects.requireNonNull(reviewId));

        if (reviewOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Submitted reviews can no longer be edited"));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Submitted reviews can no longer be deleted"));
    }

    private ResponseEntity<?> saveReviewForBooking(Long bookingId, Review review) {
        var bookingOptional = bookingRepository.findById(Objects.requireNonNull(bookingId));

        if (bookingOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Booking not found"));
        }

        var booking = bookingOptional.get();

        if (booking.getStatus() != cleanride.model.Booking.BookingStatus.COMPLETED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Can only review completed bookings"));
        }

        review.setBooking(booking);
        review.setUser(booking.getUser());
        review.setService(booking.getService());

        var existingReview = reviewRepository.findByBookingId(bookingId);
        if (existingReview.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "This booking already has a review"));
        }

        Review savedReview = reviewRepository.save(review);
        return ResponseEntity.ok(toReviewResponse(savedReview));
    }

    private Long getLongValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String value && !value.isBlank()) {
            return Long.parseLong(value);
        }
        return null;
    }

    private Integer getIntegerValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String value && !value.isBlank()) {
            return Integer.parseInt(value);
        }
        return null;
    }

    private String getStringValue(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private Map<String, Object> toReviewResponse(Review review) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", review.getId());
        response.put("bookingId", review.getBooking().getId());
        response.put("userId", review.getUser().getId());
        response.put("serviceId", review.getService().getId());
        response.put("rating", review.getRating());
        response.put("comment", review.getComment() == null ? "" : review.getComment());
        response.put("createdAt", review.getCreatedAt());
        return response;
    }
}
