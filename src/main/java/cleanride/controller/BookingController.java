package cleanride.controller;

import cleanride.model.Booking;
import cleanride.repository.BookingRepository;
import cleanride.repository.ReviewRepository;
import cleanride.repository.ServiceRepository;
import cleanride.repository.UserRepository;
import cleanride.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://*.vercel.app"})
public class BookingController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRepository serviceRepository;
    private final ReviewRepository reviewRepository;

    public BookingController(BookingRepository bookingRepository, UserRepository userRepository,
                             VehicleRepository vehicleRepository, ServiceRepository serviceRepository,
                             ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceRepository = serviceRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/user/{userId}")
    public List<Booking> getUserBookings(@PathVariable Long userId) {
        return bookingRepository.findByUserId(Objects.requireNonNull(userId))
                .stream()
                .map(this::withReviewState)
                .toList();
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBooking(@PathVariable Long bookingId) {
        var booking = bookingRepository.findById(Objects.requireNonNull(bookingId));
        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(withReviewState(booking.get()));
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) Long vehicleId,
                                           @RequestParam(required = false) Long serviceId,
                                           @RequestBody Map<String, Object> bookingData) {
        Long resolvedUserId = userId != null ? userId : getLongValue(bookingData.get("userId"));
        Long resolvedVehicleId = vehicleId != null ? vehicleId : getLongValue(bookingData.get("vehicleId"));
        Long resolvedServiceId = serviceId != null ? serviceId : getLongValue(bookingData.get("serviceId"));

        if (resolvedUserId == null || resolvedVehicleId == null || resolvedServiceId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId, vehicleId, and serviceId are required"));
        }

        var user = userRepository.findById(resolvedUserId);
        var vehicle = vehicleRepository.findById(resolvedVehicleId);
        var service = serviceRepository.findById(resolvedServiceId);

        if (user.isEmpty() || vehicle.isEmpty() || service.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID provided"));
        }

        Booking bookingDetails = new Booking();
        bookingDetails.setUser(user.get());
        bookingDetails.setVehicle(vehicle.get());
        bookingDetails.setService(service.get());
        bookingDetails.setAssignedStaff(service.get().getProvider());
        applyBookingData(bookingDetails, bookingData);

        Booking savedBooking = bookingRepository.save(bookingDetails);
        return ResponseEntity.ok(savedBooking);
    }

    @PutMapping("/{bookingId}")
    public ResponseEntity<?> updateBooking(@PathVariable Long bookingId, @RequestBody Map<String, Object> bookingData) {
        var bookingOptional = bookingRepository.findById(Objects.requireNonNull(bookingId));

        if (bookingOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Booking booking = bookingOptional.get();
        if (isLockedAfterReview(booking)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reviewed completed bookings can no longer be edited"));
        }
        applyBookingData(booking, bookingData);

        Long vehicleId = getLongValue(bookingData.get("vehicleId"));
        if (vehicleId != null) {
            var vehicle = vehicleRepository.findById(vehicleId);
            if (vehicle.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vehicle not found"));
            }
            booking.setVehicle(vehicle.get());
        }

        Long serviceId = getLongValue(bookingData.get("serviceId"));
        if (serviceId != null) {
            var service = serviceRepository.findById(serviceId);
            if (service.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Service not found"));
            }
            booking.setService(service.get());
        }

        return ResponseEntity.ok(bookingRepository.save(booking));
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long bookingId) {
        var bookingOptional = bookingRepository.findById(bookingId);
        if (bookingOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (isLockedAfterReview(bookingOptional.get())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reviewed completed bookings can no longer be deleted"));
        }

        bookingRepository.deleteById(bookingId);
        return ResponseEntity.ok(Map.of("message", "Booking deleted successfully"));
    }

    @PutMapping("/{bookingId}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long bookingId, @RequestParam Booking.BookingStatus status) {
        var bookingOptional = bookingRepository.findById(Objects.requireNonNull(bookingId));

        if (bookingOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var booking = bookingOptional.get();
        if (isLockedAfterReview(booking)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reviewed completed bookings can no longer be changed"));
        }
        booking.setStatus(status);
        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
    }

    private void applyBookingData(Booking booking, Map<String, Object> bookingData) {
        String location = getStringValue(bookingData.get("location"));
        if (location != null) {
            booking.setLocation(location);
        }

        String notes = getStringValue(bookingData.get("notes"));
        if (notes != null) {
            booking.setNotes(notes);
        }

        String bookingDate = getStringValue(bookingData.get("bookingDate"));
        if (bookingDate != null && !bookingDate.isBlank()) {
            booking.setBookingDate(LocalDate.parse(bookingDate));
        }

        String timeSlot = getStringValue(bookingData.get("timeSlot"));
        String bookingTime = timeSlot != null ? timeSlot : getStringValue(bookingData.get("bookingTime"));
        if (bookingTime != null && !bookingTime.isBlank()) {
            booking.setBookingTime(LocalTime.parse(bookingTime));
        }

        String status = getStringValue(bookingData.get("status"));
        if (status != null && !status.isBlank()) {
            booking.setStatus(Booking.BookingStatus.valueOf(status.toUpperCase()));
        }
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

    private String getStringValue(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private Booking withReviewState(Booking booking) {
        booking.setHasReview(reviewRepository.findByBookingId(booking.getId()).isPresent());
        return booking;
    }

    private boolean isLockedAfterReview(Booking booking) {
        return booking.getStatus() == Booking.BookingStatus.COMPLETED
                && reviewRepository.findByBookingId(booking.getId()).isPresent();
    }
}
