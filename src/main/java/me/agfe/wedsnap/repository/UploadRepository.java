package me.agfe.wedsnap.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class UploadRepository {

    @Value("${wedsnap.upload.base-path}")
    private String basePath;

    @Value("${wedsnap.environment}")
    private String environment;

    public String saveFile(String eventId, String uploaderName, MultipartFile file) throws IOException {
        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String newFileName = UUID.randomUUID() + ext;
        Path uploadDir = Paths.get(basePath, "event-" + eventId, uploaderName);

        // 디렉토리 생성 (권한 확인 포함)
        try {
            Files.createDirectories(uploadDir);
            log.info("[{}] Directory prepared: {}", environment, uploadDir);
        } catch (IOException e) {
            log.error("[{}] Failed to create directory: {} - {}", environment, uploadDir, e.getMessage());
            throw new IOException("디렉토리 생성 실패 (권한 확인 필요): " + uploadDir, e);
        }

        Path targetPath = uploadDir.resolve(newFileName);

        // 파일 저장
        try {
            file.transferTo(targetPath);
            log.info("[{}] File saved successfully: {} -> {}", environment, originalName, targetPath);
        } catch (IOException e) {
            log.error("[{}] Failed to save file: {} -> {} - {}", environment, originalName, targetPath, e.getMessage());
            throw new IOException("파일 저장 실패: " + originalName, e);
        }

        return newFileName;
    }

    public String findUniqueUploaderName(String eventId, String uploaderName) {
        Path baseDir = Paths.get(basePath, "event-" + eventId);
        File dir = baseDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File target = new File(dir, uploaderName);
        if (!target.exists()) {
            return uploaderName;
        }

        int counter = 1;
        String candidate;
        do {
            candidate = uploaderName + "(" + counter + ")";
            counter++;
        } while (new File(dir, candidate).exists());

        log.info("업로더명 중복 감지: {} → {}", uploaderName, candidate);
        return candidate;
    }
}
