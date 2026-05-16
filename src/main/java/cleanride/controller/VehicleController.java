package cleanride.controller;

import cleanride.model.Vehicle;
import cleanride.repository.UserRepository;
import cleanride.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "https://cleanride-frontend.vercel.app"})
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleController(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/user/{userId}")
    public List<Vehicle> getUserVehicles(@PathVariable Long userId) {
        return vehicleRepository.findByUserId(userId);
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<?> getVehicle(@PathVariable Long vehicleId) {
        var vehicle = vehicleRepository.findById(vehicleId);
        if (vehicle.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(vehicle.get());
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> addVehicle(@PathVariable Long userId, @RequestBody Vehicle vehicle) {
        var userOptional = userRepository.findById(Objects.requireNonNull(userId));

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        vehicle.setUser(userOptional.get());
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return ResponseEntity.ok(savedVehicle);
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long vehicleId, @RequestBody Vehicle vehicleDetails) {
        var vehicleOptional = vehicleRepository.findById(Objects.requireNonNull(vehicleId));

        if (vehicleOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Vehicle vehicle = vehicleOptional.get();
        vehicle.setMake(vehicleDetails.getMake());
        vehicle.setModel(vehicleDetails.getModel());
        vehicle.setYear(vehicleDetails.getYear());
        vehicle.setLicensePlate(vehicleDetails.getLicensePlate());
        vehicle.setVehicleType(vehicleDetails.getVehicleType());
        vehicle.setColor(vehicleDetails.getColor());

        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            return ResponseEntity.notFound().build();
        }

        vehicleRepository.deleteById(vehicleId);
        return ResponseEntity.ok(Map.of("message", "Vehicle deleted successfully"));
    }
}
