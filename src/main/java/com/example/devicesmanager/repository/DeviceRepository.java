package com.example.devicesmanager.repository;

import com.example.devicesmanager.model.Device;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

    // Fetch the brand in the same query: every response includes it, so a lazy load would cause N+1 queries.
    @Override
    @EntityGraph(attributePaths = "brand")
    List<Device> findAll(Specification<Device> spec);

    @Override
    @EntityGraph(attributePaths = "brand")
    Optional<Device> findById(UUID id);
}
