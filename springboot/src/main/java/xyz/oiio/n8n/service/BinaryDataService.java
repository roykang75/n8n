package xyz.oiio.n8n.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import xyz.oiio.n8n.exception.ResourceNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinaryDataService {

    @Value("${n8n.binary-data.path:${user.home}/.n8n/binaryData}")
    private String storagePath;

    private final ObjectMapper objectMapper;

    /**
     * Resolves the file path from the binaryDataId.
     * ID format: {mode}:{fileId} or just {fileId} (defaulting to filesystem-v2
     * logic)
     * e.g. "filesystem-v2:workflows/1/executions/2/binary_data/uuid"
     */
    public Resource getResource(String binaryDataId) {
        Path path = resolvePath(binaryDataId);
        File file = path.toFile();
        if (!file.exists()) {
            throw new ResourceNotFoundException("Binary data not found: " + binaryDataId);
        }
        return new FileSystemResource(file);
    }

    public BinaryMetadata getMetadata(String binaryDataId) {
        Path path = resolvePath(binaryDataId + ".metadata");
        File file = path.toFile();
        if (!file.exists()) {
            log.warn("Metadata not found for {}", binaryDataId);
            return new BinaryMetadata(); // Return empty or default
        }

        try {
            return objectMapper.readValue(file, BinaryMetadata.class);
        } catch (IOException e) {
            log.error("Failed to read metadata for {}", binaryDataId, e);
            return new BinaryMetadata();
        }
    }

    private Path resolvePath(String binaryDataId) {
        // Strip mode prefix if present (e.g., "filesystem-v2:")
        String relativePath = binaryDataId;
        if (binaryDataId.contains(":")) {
            relativePath = binaryDataId.split(":", 2)[1];
        }

        // Prevent directory traversal
        if (relativePath.contains("..")) {
            throw new IllegalArgumentException("Invalid binary ID");
        }

        return Paths.get(storagePath, relativePath);
    }

    @lombok.Data
    public static class BinaryMetadata {
        private String fileName;
        private String mimeType;
        private Long fileSize;
    }
}
