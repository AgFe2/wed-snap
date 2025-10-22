package me.agfe.wedsnap.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * 이미지 업로드 화면 컨트롤러
 */
@Slf4j
@Controller
public class UploadController {

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
