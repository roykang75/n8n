package xyz.oiio.n8n.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller to proxy icon requests to Node.js packages folder.
 * This allows the frontend to load icons from original n8n source without
 * copying files.
 * 
 * Example request: GET
 * /icons/@n8n/n8n-nodes-langchain/dist/nodes/llms/LMChatOllama/ollama.svg
 * Maps to:
 * {packagesDir}/@n8n/n8n-nodes-langchain/dist/nodes/llms/LMChatOllama/ollama.svg
 */
@RestController
@RequestMapping("/icons")
public class IconController {

    @Value("${n8n.packages.path:#{null}}")
    private String configuredPackagesPath;

    /**
     * Serve icon files from Node.js packages directory.
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> getIcon(HttpServletRequest request) {
        // Extract the path after /icons/
        String requestPath = request.getRequestURI().substring("/icons/".length());

        // Security check: prevent path traversal
        if (requestPath.isEmpty() || requestPath.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        // Transform package names to folder names
        // e.g., "@n8n/n8n-nodes-langchain" -> "@n8n/nodes-langchain"
        String transformedPath = transformPackagePath(requestPath);

        // Determine packages directory
        Path packagesDir = resolvePackagesDirectory();
        if (packagesDir == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Resolve the icon file path
        Path iconPath = packagesDir.resolve(transformedPath);

        // Security check: ensure path is within packages directory
        try {
            if (!iconPath.toRealPath().startsWith(packagesDir.toRealPath())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (IOException e) {
            // File doesn't exist or can't be resolved
            return ResponseEntity.notFound().build();
        }

        // Check if file exists
        if (!Files.exists(iconPath) || !Files.isRegularFile(iconPath)) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type
        String contentType = determineContentType(iconPath.toString());

        // Return the file
        Resource resource = new FileSystemResource(iconPath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setCacheControl("public, max-age=86400"); // Cache for 24 hours

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    /**
     * Resolve the packages directory path.
     * Tries configured path first, then falls back to relative path from current
     * directory.
     */
    private Path resolvePackagesDirectory() {
        if (configuredPackagesPath != null && !configuredPackagesPath.isEmpty()) {
            return Paths.get(configuredPackagesPath);
        }

        // Try to find packages directory relative to working directory
        // Assuming Spring Boot runs from springboot/ folder, packages is ../packages
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path packagesDir = currentDir.resolve("../packages");

        if (Files.exists(packagesDir) && Files.isDirectory(packagesDir)) {
            try {
                return packagesDir.toRealPath();
            } catch (IOException e) {
                return null;
            }
        }

        // Fallback: try from project root
        packagesDir = currentDir.resolve("packages");
        if (Files.exists(packagesDir) && Files.isDirectory(packagesDir)) {
            try {
                return packagesDir.toRealPath();
            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Determine content type based on file extension.
     */
    private String determineContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerPath.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }

    /**
     * Transform package names in the path to folder names.
     * Handles cases where npm package name differs from folder name.
     * 
     * Examples:
     * - "@n8n/n8n-nodes-langchain/..." -> "@n8n/nodes-langchain/..."
     * - "n8n-nodes-base/..." -> "nodes-base/..."
     */
    private String transformPackagePath(String requestPath) {
        // Handle @n8n/n8n-* packages (folder doesn't have n8n- prefix)
        String result = requestPath.replaceFirst("@n8n/n8n-", "@n8n/");

        // Handle n8n-nodes-base (becomes nodes-base)
        if (result.startsWith("n8n-nodes-base/")) {
            result = result.replaceFirst("n8n-nodes-base/", "nodes-base/");
        }

        // Handle n8n-core (becomes core)
        if (result.startsWith("n8n-core/")) {
            result = result.replaceFirst("n8n-core/", "core/");
        }

        // Handle n8n-workflow (becomes workflow)
        if (result.startsWith("n8n-workflow/")) {
            result = result.replaceFirst("n8n-workflow/", "workflow/");
        }

        return result;
    }
}
