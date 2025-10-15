package me.agfe.wedsnap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import me.agfe.wedsnap.validation.NoProfanity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadRequest {

    @NotBlank(message = "eventId 는 필수입니다.")
    private String eventId;

    @NotBlank(message = "업로드 사용자는 필수입니다.")
    @Pattern(
            regexp = "^[가-힣a-zA-Z0-9]{2,20}$",
            message = "이름에는 특수문자나 공백을 포함할 수 없습니다."
    )
    @NoProfanity
    private String uploaderName;
}
