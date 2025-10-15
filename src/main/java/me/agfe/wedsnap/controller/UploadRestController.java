package me.agfe.wedsnap.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.UploadRequest;
import me.agfe.wedsnap.dto.UploadResponse;
import me.agfe.wedsnap.service.UploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UploadRestController {

    private final UploadService uploadService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<UploadResponse> upload(
            @Valid @RequestPart("info") UploadRequest request,
            @RequestParam("files") List<MultipartFile> files
    ) {

        log.info("Upload request received: eventId={}, uploader={}, files={}",
                request.getEventId(), request.getUploaderName(), files);

        UploadResponse response = uploadService.processUpload(request, files);
        return ResponseEntity.ok(response);
    }
}

