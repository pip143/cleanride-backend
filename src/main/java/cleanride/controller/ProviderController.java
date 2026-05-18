package cleanride.controller;

import cleanride.model.Booking;
import cleanride.model.Review;
import cleanride.model.Service;
import cleanride.model.User;
import cleanride.repository.BookingRepository;
import cleanride.repository.ReviewRepository;
import cleanride.repository.ServiceRepository;
import cleanride.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/provider")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://*.vercel.app"})
public class ProviderController {

    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    public ProviderController(UserRepository userRepository, ServiceRepository serviceRepository,
                            BookingRepository bookingRepository, ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Get provider dashboard statistics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(@RequestParam Long providerId) {
        try {
            // Verify provider exists
            Optional<User> provider = userRepository.findById(Objects.requireNonNull(providerId));
            if (provider.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Get provider's bookings
            List<Booking> bookings = getProviderBookings(providerId);

            long totalBookings = bookings.size();
            long pendingBookings = bookings.stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.PENDING)
                    .count();

            // Calculate average rating from reviews for bookings assigned to this provider
            List<Review> providerReviews = reviewRepository.findAll()
                    .stream()
                    .filter(r -> bookings.stream().anyMatch(b -> b.getId().equals(r.getBooking().getId())))
                    .collect(Collectors.toList());

            double averageRating = providerReviews.isEmpty() ? 0 :
                    providerReviews.stream()
                            .mapToInt(Review::getRating)
                            .average()
                            .orElse(0);

            // Count unique services from assigned bookings
            long totalServices = bookings.stream()
                    .map(b -> b.getService().getId())
                    .distinct()
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBookings", totalBookings);
            stats.put("pendingBookings", pendingBookings);
            stats.put("totalServices", totalServices);
            stats.put("averageRating", Math.round(averageRating * 10.0) / 10.0);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load dashboard stats"));
        }
    }

    /**
     * Get provider's recent bookings
     */
    @GetMapping("/dashboard/recent-bookings")
    public ResponseEntity<?> getRecentBookings(@RequestParam Long providerId) {
        try {
            List<Booking> bookings = getProviderBookings(providerId)
                    .stream()
                    .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                    .limit(5)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load recent bookings"));
        }
    }

    /**
     * Get all bookings for provider
     */
    @GetMapping("/bookings")
    public ResponseEntity<?> getBookings(@RequestParam Long providerId,
                                        @RequestParam(required = false) String status) {
        try {
            List<Booking> bookings = getProviderBookings(providerId);

            if (status != null && !status.isEmpty()) {
                try {
                    Booking.BookingStatus statusEnum = Booking.BookingStatus.valueOf(status.toUpperCase());
                    bookings = bookings.stream()
                            .filter(b -> b.getStatus() == statusEnum)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    // Invalid status, return all bookings
                }
            }

            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load bookings"));
        }
    }

    @GetMapping("/bookings/stats")
    public ResponseEntity<?> getBookingStats(@RequestParam Long providerId) {
        try {
            List<Booking> bookings = getProviderBookings(providerId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBookings", bookings.size());
            stats.put("pendingBookings", bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).count());
            stats.put("confirmedBookings", bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED).count());
            stats.put("inProgressBookings", bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.IN_PROGRESS).count());
            stats.put("completedBookings", bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count());
            stats.put("cancelledBookings", bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED).count());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load booking stats"));
        }
    }

    /**
     * Get single booking details
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<?> getBookingDetails(@PathVariable Long bookingId) {
        try {
            Optional<Booking> booking = bookingRepository.findById(Objects.requireNonNull(bookingId));
            if (booking.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(booking.get());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load booking"));
        }
    }

    @PatchMapping("/bookings/{bookingId}/accept")
    public ResponseEntity<?> acceptBooking(@PathVariable Long bookingId) {
        return updateBookingStatus(bookingId, Booking.BookingStatus.CONFIRMED);
    }

    @PatchMapping("/bookings/{bookingId}/reject")
    public ResponseEntity<?> rejectBooking(@PathVariable Long bookingId) {
        return updateBookingStatus(bookingId, Booking.BookingStatus.CANCELLED);
    }

    @PatchMapping("/bookings/{bookingId}/complete")
    public ResponseEntity<?> completeBooking(@PathVariable Long bookingId) {
        return updateBookingStatus(bookingId, Booking.BookingStatus.COMPLETED);
    }

    /**
     * Get provider's reviews
     */
    @GetMapping("/reviews")
    public ResponseEntity<?> getReviews(@RequestParam Long providerId) {
        try {
            // Get provider's bookings
            List<Booking> bookings = getProviderBookings(providerId);

            // Get reviews for those bookings
            List<Review> reviews = reviewRepository.findAll()
                    .stream()
                    .filter(r -> bookings.stream().anyMatch(b -> b.getId().equals(r.getBooking().getId())))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load reviews"));
        }
    }

    /**
     * Get reviews statistics
     */
    @GetMapping("/reviews/stats")
    public ResponseEntity<?> getReviewsStats(@RequestParam Long providerId) {
        try {
            // Get provider's bookings
            List<Booking> bookings = getProviderBookings(providerId);

            // Get reviews for those bookings
            List<Review> reviews = reviewRepository.findAll()
                    .stream()
                    .filter(r -> bookings.stream().anyMatch(b -> b.getId().equals(r.getBooking().getId())))
                    .collect(Collectors.toList());

            double averageRating = reviews.isEmpty() ? 0 :
                    reviews.stream()
                            .mapToInt(Review::getRating)
                            .average()
                            .orElse(0);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalReviews", reviews.size());
            stats.put("averageRating", Math.round(averageRating * 10.0) / 10.0);
            stats.put("fiveStarCount", reviews.stream().filter(r -> r.getRating() == 5).count());
            stats.put("fourStarCount", reviews.stream().filter(r -> r.getRating() == 4).count());
            stats.put("threeStarCount", reviews.stream().filter(r -> r.getRating() == 3).count());
            stats.put("twoStarCount", reviews.stream().filter(r -> r.getRating() == 2).count());
            stats.put("oneStarCount", reviews.stream().filter(r -> r.getRating() == 1).count());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load review stats"));
        }
    }

    @PostMapping("/reviews/{reviewId}/flag")
    public ResponseEntity<?> flagReview(@PathVariable Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "Review flagged for moderation"));
    }

    /**
     * Get provider earnings
     */
    @GetMapping("/earnings")
    public ResponseEntity<?> getEarnings(@RequestParam Long providerId) {
        try {
            List<Booking> bookings = getProviderBookings(providerId)
                    .stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
                    .filter(b -> b.getPaymentStatus() == Booking.PaymentStatus.PAID)
                    .collect(Collectors.toList());

            BigDecimal totalEarnings = bookings.stream()
                    .map(b -> b.getService().getPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthEarnings = calculateMonthEarnings(bookings);

            Map<String, Object> earnings = new HashMap<>();
            earnings.put("totalEarnings", totalEarnings.doubleValue());
            earnings.put("thisMonthEarnings", monthEarnings.doubleValue());
            earnings.put("completedBookings", bookings.size());
            BigDecimal pendingEarnings = getProviderBookings(providerId)
                    .stream()
                    .filter(b -> b.getPaymentStatus() == Booking.PaymentStatus.UNPAID)
                    .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED || b.getStatus() == Booking.BookingStatus.COMPLETED)
                    .map(b -> b.getService().getPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            earnings.put("pendingEarnings", pendingEarnings.doubleValue());
            earnings.put("withdrawnEarnings", 0.0);

            return ResponseEntity.ok(earnings);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load earnings"));
        }
    }

    /**
     * Get payment history
     */
    @GetMapping("/payments")
    public ResponseEntity<?> getPaymentHistory(@RequestParam Long providerId,
                                              @RequestParam(defaultValue = "50") int limit,
                                              @RequestParam(defaultValue = "0") int offset) {
        try {
            List<Booking> bookings = getProviderBookings(providerId)
                    .stream()
                    .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load payment history"));
        }
    }

    /**
     * Get payouts (mock data for now)
     */
    @GetMapping("/payouts")
    public ResponseEntity<?> getPayouts(@RequestParam Long providerId) {
        try {
            List<Map<String, Object>> payouts = new ArrayList<>();
            // Return empty list for now - payouts table not implemented
            return ResponseEntity.ok(payouts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load payouts"));
        }
    }

    @PostMapping("/payouts/request")
    public ResponseEntity<?> requestPayout(@RequestBody Map<String, Object> payoutData) {
        Object amount = payoutData.getOrDefault("amount", 0);
        return ResponseEntity.ok(Map.of(
                "message", "Payout request submitted",
                "amount", amount
        ));
    }

    /**
     * Get provider balance
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestParam Long providerId) {
        try {
            List<Booking> completedBookings = getProviderBookings(providerId)
                    .stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
                    .filter(b -> b.getPaymentStatus() == Booking.PaymentStatus.PAID)
                    .collect(Collectors.toList());

            BigDecimal balance = completedBookings.stream()
                    .map(b -> b.getService().getPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> balanceData = new HashMap<>();
            balanceData.put("balance", balance.doubleValue());
            balanceData.put("currency", "USD");

            return ResponseEntity.ok(balanceData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load balance"));
        }
    }

    /**
     * Get services for a provider (all services created by the provider)
     */
    @GetMapping("/{providerId}/services")
    public ResponseEntity<?> getProviderServices(@PathVariable Long providerId) {
        try {
            List<Service> services = serviceRepository.findAll()
                    .stream()
                    .filter(s -> s.getProvider() != null && s.getProvider().getId().equals(providerId))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load services"));
        }
    }

    /**
     * Create a new service for the provider
     */
    @PostMapping("/services")
    public ResponseEntity<?> createService(@RequestBody Map<String, Object> serviceData) {
        try {
            String name = (String) serviceData.get("name");
            String description = (String) serviceData.get("description");
            Object priceObj = serviceData.get("price");
            Integer duration = getIntegerValue(serviceData.get("duration"));
            Long providerId = getLongValue(serviceData.get("providerId"));

            if (name == null || priceObj == null || providerId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required fields: name, price, providerId"));
            }

            Optional<User> provider = userRepository.findById(Objects.requireNonNull(providerId));
            if (provider.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Provider not found"));
            }

            BigDecimal price = new BigDecimal(priceObj.toString());
            
            Service service = new Service();
            service.setName(name);
            service.setDescription(description != null ? description : "");
            service.setPrice(price);
            service.setDuration(duration != null ? duration : 60);
            service.setProvider(provider.get());
            service.setCreatedAt(java.time.LocalDateTime.now());
            service.setUpdatedAt(java.time.LocalDateTime.now());

            Service savedService = serviceRepository.save(service);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedService.getId());
            response.put("name", savedService.getName());
            response.put("description", savedService.getDescription());
            response.put("price", savedService.getPrice().doubleValue());
            response.put("duration", savedService.getDuration());
            response.put("createdAt", savedService.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create service: " + e.getMessage()));
        }
    }

    // Helper method to calculate this month's earnings
    private BigDecimal calculateMonthEarnings(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> isCurrentMonth(b.getCreatedAt()))
                .map(b -> b.getService().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isCurrentMonth(java.time.LocalDateTime dateTime) {
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        java.time.YearMonth bookingMonth = java.time.YearMonth.from(dateTime);
        return currentMonth.equals(bookingMonth);
    }

    private Long getLongValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer getIntegerValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private List<Booking> getProviderBookings(Long providerId) {
        return bookingRepository.findAll()
                .stream()
                .filter(b -> isProviderBooking(b, providerId))
                .collect(Collectors.toList());
    }

    private boolean isProviderBooking(Booking booking, Long providerId) {
        if (booking.getAssignedStaff() != null && booking.getAssignedStaff().getId().equals(providerId)) {
            return true;
        }
        if (booking.getService() == null) {
            return false;
        }
        if (booking.getService().getProvider() != null) {
            return booking.getService().getProvider().getId().equals(providerId);
        }
        return booking.getAssignedStaff() == null;
    }

    private ResponseEntity<?> updateBookingStatus(Long bookingId, Booking.BookingStatus status) {
        Optional<Booking> booking = bookingRepository.findById(Objects.requireNonNull(bookingId));
        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Booking updatedBooking = booking.get();
        if (status == Booking.BookingStatus.COMPLETED
                && updatedBooking.getPaymentStatus() != Booking.PaymentStatus.PAID) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot complete an unpaid booking"));
        }

        if (updatedBooking.getStatus() == Booking.BookingStatus.COMPLETED
                && reviewRepository.findByBookingId(updatedBooking.getId()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reviewed completed bookings can no longer be changed"));
        }
        updatedBooking.setStatus(status);
        return ResponseEntity.ok(bookingRepository.save(updatedBooking));
    }
}
