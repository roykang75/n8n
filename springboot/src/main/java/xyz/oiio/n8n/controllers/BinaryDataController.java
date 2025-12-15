package xyz.oiio.n8n.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.oiio.n8n.service.BinaryDataService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/rest/binary-data")
@RequiredArgsConstructor
public class BinaryDataController {

    private final BinaryDataService binaryDataService;

    @GetMapping
    public ResponseEntity<Resource> getBinaryData(
            @RequestParam("id") String id,
            @RequestParam(value = "action", defaultValue = "view") String action,
            @RequestParam(value = "fileName", required = false) String fileNameParam,
            @RequestParam(value = "mimeType", required = false) String mimeTypeParam) {
        Resource resource = binaryDataService.getResource(id);
        BinaryDataService.BinaryMetadata metadata = binaryDataService.getMetadata(id);

        String fileName = fileNameParam != null ? fileNameParam : metadata.getFileName();
        String mimeType = mimeTypeParam != null ? mimeTypeParam : metadata.getMimeType();

        if (mimeType == null) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentLength(metadata.getFileSize() != null ? metadata.getFileSize() : 0); // TODO: actual file size
                                                                                               // fallback

        if ("download".equals(action) && fileName != null) {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            headers.setContentDispositionFormData("attachment", encodedFileName);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
