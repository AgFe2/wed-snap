package me.agfe.wedsnap.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
import me.agfe.wedsnap.exception.ErrorCode;
import me.agfe.wedsnap.exception.WedSnapException;
import me.agfe.wedsnap.service.UploadService;
import me.agfe.wedsnap.validation.NoProfanity;

@Slf4j
@Validated
@Controller
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * 메인 업로드 페이지
     * @param model 뷰에 전달할 모델
     * @return upload.html 템플릿
     */
    @GetMapping("/")
    public String uploadPage(Model model) {
        log.info("Upload page requested");
        model.addAttribute("baseUrl", baseUrl);
        return "upload";
    }

    /**
     * 업로드 API
     * @param eventName 이벤트 이름
     * @param uploaderName 업로더 이름
     * @param files 업로드 파일들
     * @return 업로드 결과 응답
     */
    @PostMapping(value = "/api/events/{eventName}/upload")
    @ResponseBody
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
            @RequestParam(required = false) List<MultipartFile> files
    ) {
        // files 파라미터 검증
        if (files == null || files.isEmpty()) {
            throw new WedSnapException(ErrorCode.NO_FILES_PROVIDED);
        }

        // 빈 파일 검증
        if (files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new WedSnapException(ErrorCode.ALL_FILES_EMPTY);
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

