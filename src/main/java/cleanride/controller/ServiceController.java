package cleanride.controller;

import cleanride.model.Service;
import cleanride.dto.ServiceDTO;
import cleanride.repository.ServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175"})
public class ServiceController {

    private final ServiceRepository serviceRepository;

    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping
    public List<ServiceDTO> getAllServices() {
        return serviceRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<?> getService(@PathVariable Long serviceId) {
        var service = serviceRepository.findById(serviceId);
        if (service.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertToDTO(service.get()));
    }

    @PostMapping
    public ServiceDTO createService(@RequestBody ServiceDTO serviceDTO) {
        Service service = new Service();
        service.setName(serviceDTO.getName());
        service.setDescription(serviceDTO.getDescription());
        service.setPrice(serviceDTO.getPrice());
        service.setDuration(serviceDTO.getDuration());
        
        Service saved = serviceRepository.save(service);
        return convertToDTO(saved);
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<?> updateService(@PathVariable Long serviceId, @RequestBody ServiceDTO serviceDTO) {
        var serviceOptional = serviceRepository.findById(serviceId);
        if (serviceOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var service = serviceOptional.get();
        service.setName(serviceDTO.getName());
        service.setDescription(serviceDTO.getDescription());
        service.setPrice(serviceDTO.getPrice());
        service.setDuration(serviceDTO.getDuration());
        return ResponseEntity.ok(convertToDTO(serviceRepository.save(service)));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<?> deleteService(@PathVariable Long serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            return ResponseEntity.notFound().build();
        }

        serviceRepository.deleteById(serviceId);
        return ResponseEntity.ok(Map.of("message", "Service deleted successfully"));
    }

    private ServiceDTO convertToDTO(Service service) {
        return new ServiceDTO(
            service.getId(),
            service.getName(),
            service.getDescription(),
            service.getPrice(),
            service.getDuration(),
            service.getProvider() != null ? service.getProvider().getId() : null,
            service.getCreatedAt(),
            service.getUpdatedAt()
        );
    }
}
