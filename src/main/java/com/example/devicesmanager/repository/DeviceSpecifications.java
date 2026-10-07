package com.example.devicesmanager.repository;

import com.example.devicesmanager.model.Device;
import com.example.devicesmanager.model.DeviceState;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class DeviceSpecifications {

    private DeviceSpecifications() {}

    public static Specification<Device> hasBrandId(UUID brandId) {
        return (root, query, cb) -> brandId == null ? null : cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Device> hasBrandNameLike(String brandName) {
        return (root, query, cb) -> {
            if (brandName == null || brandName.isBlank()) {
                return null;
            }
            String sanitized = brandName.toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String pattern = "%" + sanitized + "%";
            return cb.like(cb.lower(root.get("brand").get("name")), pattern, '\\');
        };
    }

    public static Specification<Device> hasState(DeviceState state) {
        return (root, query, cb) -> state == null ? null : cb.equal(root.get("state"), state);
    }
}
