package me.agfe.wedsnap.service;

import me.agfe.wedsnap.dto.UploadRequest;
import me.agfe.wedsnap.dto.UploadResponse;
import me.agfe.wedsnap.repository.UploadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadService 테스트")
class UploadServiceTest {

    @Mock
    private UploadRepository uploadRepository;

    @InjectMocks
    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        // environment 필드 주입
        ReflectionTestUtils.setField(uploadService, "environment", "test");
    }

    @Test
    @DisplayName("파일 업로드 성공 - 단일 파일")
    void processUpload_SingleFile_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길동";
        String uniqueUploaderName = "홍길동";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(file))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-file-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEventName()).isEqualTo(eventName);
        assertThat(response.getUploaderName()).isEqualTo(uniqueUploaderName);
        assertThat(response.getTotalFiles()).isEqualTo(1);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(0);
        assertThat(response.getFailedFiles()).isEmpty();
        assertThat(response.getMessage()).contains("1개 업로드 성공");
        assertThat(response.getTimestamp()).isNotNull();

        verify(uploadRepository, times(1)).findUniqueUploaderName(eventName, uploaderName);
        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 다중 파일")
    void processUpload_MultipleFiles_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "김철수";
        String uniqueUploaderName = "김철수";

        MockMultipartFile file1 = new MockMultipartFile(
                "file1", "image1.jpg", "image/jpeg", "content1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file2", "image2.png", "image/png", "content2".getBytes()
        );
        MockMultipartFile file3 = new MockMultipartFile(
                "file3", "image3.gif", "image/gif", "content3".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(Arrays.asList(file1, file2, file3))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getTotalFiles()).isEqualTo(3);
        assertThat(response.getSuccessCount()).isEqualTo(3);
        assertThat(response.getFailCount()).isEqualTo(0);
        assertThat(response.getFailedFiles()).isEmpty();

        verify(uploadRepository, times(3)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 빈 파일")
    void processUpload_EmptyFile_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "이영희";
        String uniqueUploaderName = "이영희";

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(emptyFile))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getTotalFiles()).isEqualTo(1);
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).contains("empty.jpg");
        assertThat(response.getMessage()).contains("0개 업로드 성공, 1개 실패");

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 파일명이 null")
    void processUpload_NullFileName_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "박민수";
        String uniqueUploaderName = "박민수";

        MockMultipartFile fileWithNullName = new MockMultipartFile(
                "file", null, "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(fileWithNullName))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).hasSize(1);

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 허용되지 않은 파일 확장자")
    void processUpload_InvalidExtension_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "최지영";
        String uniqueUploaderName = "최지영";

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "pdf content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(invalidFile))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).contains("document.pdf");

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 부분 성공 - 일부 파일만 성공")
    void processUpload_PartialSuccess() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "정다은";
        String uniqueUploaderName = "정다은";

        MockMultipartFile validFile = new MockMultipartFile(
                "file1", "valid.jpg", "image/jpeg", "valid content".getBytes()
        );
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file2", "invalid.exe", "application/exe", "invalid content".getBytes()
        );
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file3", "empty.png", "image/png", new byte[0]
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(Arrays.asList(validFile, invalidFile, emptyFile))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), eq(validFile)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getTotalFiles()).isEqualTo(3);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(2);
        assertThat(response.getFailedFiles()).containsExactlyInAnyOrder("invalid.exe", "empty.png");
        assertThat(response.getMessage()).contains("1개 업로드 성공, 2개 실패");

        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - IOException 발생")
    void processUpload_IOException_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "강민호";
        String uniqueUploaderName = "강민호";

        MockMultipartFile file = new MockMultipartFile(
                "file", "error.jpg", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(file))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenThrow(new IOException("Disk full"));

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).contains("error.jpg");
    }

    @Test
    @DisplayName("업로더명 중복 처리 확인")
    void processUpload_UniqueUploaderName_Applied() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길동";
        String uniqueUploaderName = "홍길동(1)"; // 중복 처리된 이름

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(file))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getUploaderName()).isEqualTo(uniqueUploaderName);
        assertThat(request.getUploaderName()).isEqualTo(uniqueUploaderName); // request 객체도 업데이트되어야 함

        verify(uploadRepository, times(1)).findUniqueUploaderName(eventName, uploaderName);
        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 확장자 대소문자 구분 없이 검증")
    void processUpload_CaseInsensitiveExtension_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "김수진";
        String uniqueUploaderName = "김수진";

        MockMultipartFile upperCaseFile = new MockMultipartFile(
                "file", "photo.JPG", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(upperCaseFile))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(0);

        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("HEIF 확장자 파일 업로드 성공")
    void processUpload_HeifExtension_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "이지훈";
        String uniqueUploaderName = "이지훈";

        MockMultipartFile heifFile = new MockMultipartFile(
                "file", "iphone-photo.heif", "image/heif", "heif content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(heifFile))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.heif");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(0);

        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("확장자가 없는 파일 업로드 실패")
    void processUpload_NoExtension_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "박서연";
        String uniqueUploaderName = "박서연";

        MockMultipartFile noExtFile = new MockMultipartFile(
                "file", "noextension", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                .eventName(eventName)
                .uploaderName(uploaderName)
                .files(List.of(noExtFile))
                .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).contains("noextension");

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }
}
