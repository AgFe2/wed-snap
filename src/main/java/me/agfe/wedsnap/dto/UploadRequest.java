package me.agfe.wedsnap.dto;

import org.springframework.web.multipart.MultipartFile;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadRequest {
    private String eventId;
    private String uploaderName;
    private List<MultipartFile> files;
}
