package me.agfe.wedsnap.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.CommonApiResponse;
import me.agfe.wedsnap.dto.UploadRequest;
import me.agfe.wedsnap.dto.UploadResponse;
import me.agfe.wedsnap.service.UploadService;
import me.agfe.wedsnap.validation.NoProfanity;

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
                    regexp = "^[가-힣a-zA-Z0-9]*$",
                    message = "이름에는 특수문자나 공백을 포함할 수 없습니다."
            )
            @Size(
                    min = 2,
                    max = 20,
                    message = "이름은 2자 이상 20자 이하로 입력해야 합니다."
            )
            @RequestParam String uploaderName,

            @Size(
                    min = 1,
                    max = 20,
                    message = "사진은 최소 1장부터 최대 20장까지만 업로드 가능합니다."
            )
            @RequestParam List<MultipartFile> files
    ) {
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ConstraintViolationException("최소 1개의 사진을 업로드해야 합니다.", null);
        }

        log.info("Upload request received: eventId={}, uploader={}, files={}",
                 eventName, uploaderName, files.size());

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(files)
                                             .build();

        UploadResponse response = uploadService.processUpload(request);
        return CommonApiResponse.success(response);
    }
}

