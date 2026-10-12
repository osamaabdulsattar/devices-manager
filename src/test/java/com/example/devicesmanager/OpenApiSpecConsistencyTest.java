package com.example.devicesmanager;

import com.example.devicesmanager.dto.BrandRequest;
import com.example.devicesmanager.dto.BrandResponse;
import com.example.devicesmanager.dto.DeviceCreateRequest;
import com.example.devicesmanager.dto.DevicePatchRequest;
import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.dto.DeviceUpdateRequest;
import com.example.devicesmanager.dto.ErrorResponse;
import com.example.devicesmanager.dto.FieldErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The OpenAPI spec is hand-written (contract first), so this test keeps it from drifting away from the code:
 * every documented operation must be implemented, every implemented operation must be documented, and the
 * documented schemas must have the same fields as the DTOs they describe.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@SuppressWarnings("unchecked")
class OpenApiSpecConsistencyTest {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    private static final Map<String, Class<? extends Record>> SCHEMA_TYPES = Map.of(
            "Brand", BrandResponse.class,
            "BrandRequest", BrandRequest.class,
            "DeviceResponse", DeviceResponse.class,
            "DeviceCreateRequest", DeviceCreateRequest.class,
            "DeviceUpdateRequest", DeviceUpdateRequest.class,
            "DevicePatchRequest", DevicePatchRequest.class,
            "ErrorResponse", ErrorResponse.class,
            "FieldError", FieldErrorResponse.class
    );

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void documentedOperationsMatchImplementedEndpoints() throws IOException {
        Map<String, Object> spec = loadSpec();
        String basePath = (String) ((List<Map<String, Object>>) spec.get("servers")).getFirst().get("url");

        Set<String> documented = new HashSet<>();
        ((Map<String, Map<String, Object>>) spec.get("paths")).forEach((path, operations) ->
                operations.keySet().stream()
                        .filter(HTTP_METHODS::contains)
                        .forEach(method -> documented.add(method.toUpperCase() + " " + basePath + path)));

        Set<String> implemented = new HashSet<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            if (!handler.getBeanType().getPackageName().startsWith("com.example.devicesmanager")) {
                return;
            }
            info.getPathPatternsCondition().getPatternValues().forEach(pattern ->
                    info.getMethodsCondition().getMethods().forEach(method ->
                            implemented.add(method.name() + " " + pattern)));
        });

        assertThat(implemented).isEqualTo(documented);
    }

    @Test
    void documentedSchemasMatchDtoFields() throws IOException {
        Map<String, Object> spec = loadSpec();
        Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>)
                ((Map<String, Object>) spec.get("components")).get("schemas");

        SCHEMA_TYPES.forEach((schemaName, type) -> {
            Map<String, Object> properties = (Map<String, Object>) schemas.get(schemaName).get("properties");
            Set<String> fields = Arrays.stream(type.getRecordComponents())
                    .map(RecordComponent::getName)
                    .collect(Collectors.toSet());

            assertThat(properties.keySet())
                    .as("properties of schema %s vs fields of %s", schemaName, type.getSimpleName())
                    .isEqualTo(fields);
        });
    }

    private static Map<String, Object> loadSpec() throws IOException {
        try (InputStream in = new ClassPathResource("static/openapi.yaml").getInputStream()) {
            return new Yaml().load(in);
        }
    }
}
