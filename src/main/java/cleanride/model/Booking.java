package cleanride.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    private String location;
    private String notes;
    private LocalDate bookingDate;
    private LocalTime bookingTime;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    private Boolean hasReview = false;

    public enum BookingStatus { PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED }
    public enum PaymentStatus { UNPAID, PAID, REFUNDED }

    public Booking() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }
    public User getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(User assignedStaff) { this.assignedStaff = assignedStaff; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    public LocalTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalTime bookingTime) { this.bookingTime = bookingTime; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getHasReview() { return hasReview; }
    public void setHasReview(Boolean hasReview) { this.hasReview = hasReview; }
}
