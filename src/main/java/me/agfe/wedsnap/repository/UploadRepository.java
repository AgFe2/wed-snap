package me.agfe.wedsnap.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Repository
public class UploadRepository {

    @Value("${wedsnap.upload.base-path:/nas/wedsnap}")
    private String basePath;

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
        log.info("✅ File saved: {} -> {}", originalName, targetPath);

        return newFileName;
    }
}
