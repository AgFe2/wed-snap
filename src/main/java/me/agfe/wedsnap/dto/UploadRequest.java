package me.agfe.wedsnap.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadRequest {
    private String eventName;
    private String uploaderName;
    private List<MultipartFile> files;
}
