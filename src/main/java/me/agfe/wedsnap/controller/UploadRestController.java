package me.agfe.wedsnap.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.UploadRequest;
import me.agfe.wedsnap.dto.UploadResponse;
import me.agfe.wedsnap.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UploadRestController {

    private final UploadService uploadService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam(required = false, defaultValue = "test") String eventId,
            @RequestParam String uploaderName,
            @RequestParam("files") List<MultipartFile> files
    ) {
        UploadRequest request = UploadRequest.builder()
                .eventId(eventId)
                .uploaderName(uploaderName)
                .files(files)
                .build();

        log.info("Upload request received: eventId={}, uploader={}, files={}",
                eventId, uploaderName, files.size());

        UploadResponse response = uploadService.processUpload(request);
        return ResponseEntity.ok(response);
    }
}

