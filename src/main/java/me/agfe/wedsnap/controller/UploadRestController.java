package me.agfe.wedsnap.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.CommonApiResponse;
import me.agfe.wedsnap.dto.UploadRequest;
import me.agfe.wedsnap.dto.UploadResponse;
import me.agfe.wedsnap.service.UploadService;
import me.agfe.wedsnap.validation.NoProfanity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UploadRestController {

    private final UploadService uploadService;

    @PostMapping(value = "/events/{eventName}/upload")
    public CommonApiResponse<UploadResponse> upload(
            @PathVariable String eventName,

            @NotBlank
            @NoProfanity
            @Pattern(
                    regexp = "^[가-힣a-zA-Z0-9]{2,20}$",
                    message = "이름에는 특수문자나 공백을 포함할 수 없습니다."
            )
            @RequestParam String uploaderName,

            @RequestParam List<MultipartFile> files
    ) {
        log.info("Upload request received: eventId={}, uploader={}, files={}",
                eventName, uploaderName, files != null ? files.size() : 0);

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(files)
                .build();

        UploadResponse response = uploadService.processUpload(request);
        return CommonApiResponse.success(response);
    }
}

