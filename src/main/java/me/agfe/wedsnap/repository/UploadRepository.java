package me.agfe.wedsnap.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

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
        Files.createDirectories(uploadDir);

        Path targetPath = uploadDir.resolve(newFileName);
        file.transferTo(targetPath);
        log.info("[{}] File saved: {} -> {}", environment, originalName, targetPath);

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
