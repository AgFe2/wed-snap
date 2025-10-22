package me.agfe.wedsnap.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.extern.slf4j.Slf4j;

/**
 * 이미지 업로드 화면 컨트롤러
 */
@Slf4j
@Controller
public class UploadController {

    @Value("${app.version}")
    private String appVersion;

    /**
     * 모든 뷰에 앱 버전을 자동으로 추가 (캐시 무효화용)
     */
    @ModelAttribute("appVersion")
    public String addAppVersion() {
        log.info("App version: {}", appVersion);
        return appVersion;
    }

    /**
     * 메인 업로드 페이지
     * @return upload.html 템플릿
     */
    @GetMapping("/")
    public String uploadPage() {
        log.info("Upload page requested");
        return "upload";
    }
}
