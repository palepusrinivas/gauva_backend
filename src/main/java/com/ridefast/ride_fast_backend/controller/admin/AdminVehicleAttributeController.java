package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import com.ridefast.ride_fast_backend.repository.v2.VehicleCategoryRepository;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/vehicle/attribute-setup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminVehicleAttributeController {

    private final VehicleCategoryRepository vehicleCategoryRepository;
    private final StorageService storageService;

    // ==================== Vehicle Categories ====================

    /**
     * Get all vehicle categories
     * GET /api/admin/vehicle/attribute-setup/category
     */
    @GetMapping("/category")
    public ResponseEntity<?> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<VehicleCategory> categories = vehicleCategoryRepository.findAll(pageable);
            
            // Map to response format expected by frontend
            List<Map<String, Object>> categoryList = categories.getContent().stream()
                    .map(cat -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", cat.getId());
                        map.put("categoryName", cat.getName()); // Frontend expects categoryName
                        map.put("name", cat.getName());
                        map.put("description", cat.getDescription());
                        map.put("type", cat.getType());
                        map.put("image", cat.getImage());
                        map.put("active", cat.getIsActive() != null ? cat.getIsActive() : true);
                        map.put("isActive", cat.getIsActive() != null ? cat.getIsActive() : true);
                        return map;
                    })
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", categoryList);
            response.put("totalElements", categories.getTotalElements());
            response.put("totalPages", categories.getTotalPages());
            response.put("size", categories.getSize());
            response.put("number", categories.getNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching vehicle categories: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch vehicle categories: " + e.getMessage()));
        }
    }

    /**
     * Get vehicle category by ID
     * GET /api/admin/vehicle/attribute-setup/category/{id}
     */
    @GetMapping("/category/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable String id) {
        try {
            VehicleCategory category = vehicleCategoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", category.getId());
            response.put("categoryName", category.getName());
            response.put("name", category.getName());
            response.put("description", category.getDescription());
            response.put("type", category.getType());
            response.put("image", category.getImage());
            response.put("active", category.getIsActive() != null ? category.getIsActive() : true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching category {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Category not found: " + e.getMessage()));
        }
    }

    /**
     * Create vehicle category
     * POST /api/admin/vehicle/attribute-setup/category
     */
    @PostMapping(value = "/category", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCategory(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "image", required = false) String imageUrl) {
        try {
            // Check if category with same name already exists
            if (vehicleCategoryRepository.findByName(name).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Category with name '" + name + "' already exists"));
            }

            VehicleCategory category = VehicleCategory.builder()
                    .id(UUID.randomUUID().toString())
                    .name(name)
                    .description(description)
                    .type(type)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imagePath = "vehicle-categories/" + category.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + 
                            getFileExtension(imageFile.getOriginalFilename());
                    String uploadedUrl = storageService.uploadFile(imageFile, imagePath);
                    category.setImage(uploadedUrl);
                    log.info("Uploaded category image: {}", uploadedUrl);
                } catch (Exception e) {
                    log.error("Failed to upload category image: {}", e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
                }
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                category.setImage(imageUrl);
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Category image is required"));
            }

            VehicleCategory saved = vehicleCategoryRepository.save(category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("categoryName", saved.getName());
            response.put("name", saved.getName());
            response.put("description", saved.getDescription());
            response.put("type", saved.getType());
            response.put("image", saved.getImage());
            response.put("active", saved.getIsActive());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating vehicle category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create category: " + e.getMessage()));
        }
    }

    /**
     * Update vehicle category
     * PUT /api/admin/vehicle/attribute-setup/category/{id}
     */
    @PutMapping(value = "/category/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCategory(
            @PathVariable String id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            VehicleCategory category = vehicleCategoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            if (name != null && !name.isBlank()) {
                // Check if another category with this name exists
                vehicleCategoryRepository.findByName(name)
                        .ifPresent(existing -> {
                            if (!existing.getId().equals(id)) {
                                throw new RuntimeException("Category with name '" + name + "' already exists");
                            }
                        });
                category.setName(name);
            }
            if (description != null) {
                category.setDescription(description);
            }
            if (type != null && !type.isBlank()) {
                category.setType(type);
            }
            if (isActive != null) {
                category.setIsActive(isActive);
            }
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imagePath = "vehicle-categories/" + category.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + 
                            getFileExtension(imageFile.getOriginalFilename());
                    String uploadedUrl = storageService.uploadFile(imageFile, imagePath);
                    category.setImage(uploadedUrl);
                } catch (Exception e) {
                    log.error("Failed to upload category image: {}", e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
                }
            }

            category.setUpdatedAt(LocalDateTime.now());
            VehicleCategory saved = vehicleCategoryRepository.save(category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("categoryName", saved.getName());
            response.put("name", saved.getName());
            response.put("description", saved.getDescription());
            response.put("type", saved.getType());
            response.put("image", saved.getImage());
            response.put("active", saved.getIsActive());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating category {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update category: " + e.getMessage()));
        }
    }

    /**
     * Delete vehicle category
     * DELETE /api/admin/vehicle/attribute-setup/category/{id}
     */
    @DeleteMapping("/category/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable String id) {
        try {
            if (!vehicleCategoryRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Category not found"));
            }
            
            VehicleCategory category = vehicleCategoryRepository.findById(id).orElse(null);
            if (category != null) {
                category.setDeletedAt(LocalDateTime.now());
                category.setIsActive(false);
                vehicleCategoryRepository.save(category);
            }
            
            return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting category {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete category: " + e.getMessage()));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}

