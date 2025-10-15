package me.agfe.wedsnap.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.*;
import me.agfe.wedsnap.repository.UploadRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final UploadRepository uploadRepository;

    public UploadResponse processUpload(UploadRequest request) {
        List<MultipartFile> files = request.getFiles();
        List<String> failedFiles = new ArrayList<>();
        int successCount = 0;

        synchronized (this) {
            for (MultipartFile file : files) {
                try {
                    if (file.isEmpty()) {
                        failedFiles.add(file.getOriginalFilename());
                        continue;
                    }
                    uploadRepository.saveFile(request.getEventId(), request.getUploaderName(), file);
                    successCount++;
                } catch (IOException e) {
                    log.error("❌ Failed to save file: {}", file.getOriginalFilename(), e);
                    failedFiles.add(file.getOriginalFilename());
                }
            }
        }

        int total = files.size();
        int failCount = failedFiles.size();

        return UploadResponse.builder()
                .eventId(request.getEventId())
                .uploaderName(request.getUploaderName())
                .totalFiles(total)
                .successCount(successCount)
                .failCount(failCount)
                .failedFiles(failedFiles)
                .timestamp(LocalDateTime.now())
                .message(String.format("%d개 업로드 성공, %d개 실패", successCount, failCount))
                .build();
    }
}