package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.school.Institution;
import com.ridefast.ride_fast_backend.repository.school.InstitutionRepository;
import com.ridefast.ride_fast_backend.service.school.InstitutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;
    private final InstitutionRepository institutionRepository;

    @PostMapping
    public ResponseEntity<Institution> create(@RequestBody @Valid CreateInstitutionRequest req) {
        Institution inst = new Institution();
        inst.setName(req.getName());
        inst.setUniqueId(institutionService.generateUniqueId());
        inst.setPrimaryContactName(req.getPrimaryContactName());
        inst.setPrimaryContactPhone(req.getPrimaryContactPhone());
        inst.setEmail(req.getEmail());
        inst.setGstNumber(req.getGstNumber());
        Institution saved = institutionService.create(inst);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/{UniqueId}")
    public ResponseEntity<Institution> get(@PathVariable String  UniqueId) {
        Optional<Institution> inst = institutionService.findById(UniqueId);
        return inst.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("institutes")
    public ResponseEntity<List<Institution>> getAll() {
        List<Institution> inst = institutionRepository.findAll();
        return new ResponseEntity<>(inst, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Institution> update(@PathVariable Long id, @RequestBody CreateInstitutionRequest req) {
        Optional<Institution> opt = institutionService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Institution updates = new Institution();
        updates.setName(req.getName());
        updates.setPrimaryContactName(req.getPrimaryContactName());
        updates.setPrimaryContactPhone(req.getPrimaryContactPhone());
        updates.setEmail(req.getEmail());
        updates.setGstNumber(req.getGstNumber());
        return ResponseEntity.ok(institutionService.update(opt.get(), updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Optional<Institution> opt = institutionService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        institutionService.delete(opt.get());
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateInstitutionRequest {
        @NotBlank
        private String name;
        private String primaryContactName;
        private String primaryContactPhone;
        private String email;
        private String gstNumber;
    }
}


